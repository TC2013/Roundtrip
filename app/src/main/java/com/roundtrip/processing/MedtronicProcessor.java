package com.roundtrip.processing;

import com.roundtrip.decoding.packages.MedtronicReading;
import com.roundtrip.decoding.packages.MeterReading;
import com.roundtrip.decoding.packages.SensorReading;
import com.roundtrip.decoding.packages.SensorWarmupReading;
import com.roundtrip.enlite.calibration.CalibrationPair;
import com.roundtrip.enlite.calibration.LinearRegressionCalibration;
import com.roundtrip.enlite.calibration.OnePointCalibration;
import com.roundtrip.enlite.calibration.TwoPointCalibration;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public class MedtronicProcessor {
    private final int KEEP_READINGS = 10;

    final TreeSet<CalibrationPair> knownCalibrations;
    final CircularFifoQueue<SensorReading> lastSensorReadings;
    final CircularFifoQueue<MeterReading> lastGlucoseReadings;

    private static MedtronicProcessor instance;

    public static MedtronicProcessor getInstance() {
        if (instance == null) {
            synchronized (MedtronicProcessor.class) {
                if (instance == null) {
                    instance = new MedtronicProcessor();
                }
            }
        }
        return instance;
    }

    public MedtronicProcessor() {
        this.knownCalibrations = new TreeSet<>();
        this.lastSensorReadings = new CircularFifoQueue<>(KEEP_READINGS);
        this.lastGlucoseReadings = new CircularFifoQueue<>(KEEP_READINGS);
    }

    public void process(final MedtronicReading packet) {
        if (packet instanceof SensorReading) {
            SensorReading sensorReading = (SensorReading) packet;

            if (!this.lastSensorReadings.contains(sensorReading)) {
                this.lastSensorReadings.add(sensorReading);

                // Look if it possible to convert the SGV to a Glucose Level based on the calibration scheme
                int calibrations = this.knownCalibrations.size();

                final double approximatedGlucoseLevel;
                if (calibrations == 0) {
                    // NEED CALIBRATION FIRST
                    approximatedGlucoseLevel = -1;
                } else if (calibrations == 1) {
                    approximatedGlucoseLevel = new OnePointCalibration().approximateGlucoseLevel(sensorReading, this.knownCalibrations);
                } else if (calibrations == 2) {
                    approximatedGlucoseLevel = new TwoPointCalibration().approximateGlucoseLevel(sensorReading, this.knownCalibrations);
                } else if (calibrations >= 3) {
                    approximatedGlucoseLevel = new LinearRegressionCalibration().approximateGlucoseLevel(sensorReading, this.knownCalibrations);
                }
            }

        } else if (packet instanceof SensorWarmupReading) {
            // If the sensor is warming up, we need new calibrations
            this.knownCalibrations.clear();
        } else if (packet instanceof MeterReading) {
            MeterReading glucoseReading = (MeterReading) packet;

            // Check if it is new material
            if (!this.lastGlucoseReadings.contains(glucoseReading)) {
                this.lastGlucoseReadings.add(glucoseReading);

                // Look if the Meter value can be paired with a sensor value to create a calibration point
                long secondsDifference = Long.MAX_VALUE;
                SensorReading nearestSensorReading = null;
                for (SensorReading sensorReading : this.lastSensorReadings) {
                    long diff = sensorReading.createdDifference(glucoseReading);
                    if (diff < secondsDifference) {
                        secondsDifference = diff;
                        nearestSensorReading = sensorReading;
                    }
                }

                // Check if they are close enough
                if (secondsDifference < MedtronicReading.EXPIRATION_FOUR_MINUTES) {
                    this.knownCalibrations.add(new CalibrationPair(nearestSensorReading, glucoseReading));
                }
            }

        } else {
            // Different packages...
        }
    }

    public Queue<MeterReading> getClucose() {
        return this.lastGlucoseReadings;
    }

    public Set<CalibrationPair> getCalibrationPairs() {
        return this.knownCalibrations;
    }

}
