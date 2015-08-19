package com.roundtrip.processing;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.roundtrip.Intents;
import com.roundtrip.decoding.packages.MedtronicReading;
import com.roundtrip.decoding.packages.MeterReading;
import com.roundtrip.decoding.packages.SensorMeasurement;
import com.roundtrip.decoding.packages.SensorReading;
import com.roundtrip.decoding.packages.SensorWarmupReading;
import com.roundtrip.enlite.calibration.CalibrationPair;
import com.roundtrip.enlite.calibration.LinearRegressionCalibration;
import com.roundtrip.enlite.calibration.OnePointCalibration;
import com.roundtrip.enlite.calibration.TwoPointCalibration;
import com.roundtrip.services.RTDemoService;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MedtronicProcessor {
    private static final String TAG = "MedtronicProcessor";
    private final int KEEP_READINGS = 10;

    final List<CalibrationPair> knownCalibrations;
    final CircularFifoQueue<SensorMeasurement> lastSensorReadings;
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
        this.knownCalibrations = new LinkedList<>();
        this.lastSensorReadings = new CircularFifoQueue<>(KEEP_READINGS);
        this.lastGlucoseReadings = new CircularFifoQueue<>(KEEP_READINGS);
    }

    public void process(final MedtronicReading packet) {
        if (packet instanceof SensorReading) {
            SensorReading sensorReading = (SensorReading) packet;
            Log.d(TAG, "Found an Enlite package");

            boolean send = false;
            for (SensorMeasurement measurement : sensorReading.getIsigMeasurements()) {
                if (!this.lastSensorReadings.contains(sensorReading)) {
                    Log.d(TAG, "Found a new measurements");
                    this.lastSensorReadings.add(measurement);

                    // Look if it possible to convert the SGV to a Glucose Level based on the calibration scheme
                    int calibrations = this.knownCalibrations.size();

                    double approximatedGlucoseLevel = -1;
                    if (calibrations == 1) {
                        approximatedGlucoseLevel = new OnePointCalibration().approximateGlucoseLevel(measurement.getIsig(), this.knownCalibrations);
                    } else if (calibrations == 2) {
                        approximatedGlucoseLevel = new TwoPointCalibration().approximateGlucoseLevel(measurement.getIsig(), this.knownCalibrations);
                    } else if (calibrations >= 3) {
                        approximatedGlucoseLevel = new LinearRegressionCalibration().approximateGlucoseLevel(measurement.getIsig(), this.knownCalibrations);
                    }

                    if (!send) {
                        send = true;
                        Intent batteryUpdate = new Intent(Intents.ENLITE_SENSOR_UPDATE);
                        batteryUpdate.putExtra("glucose", approximatedGlucoseLevel);
                        batteryUpdate.putExtra("isig", measurement.getIsig());
                        batteryUpdate.putExtra("battery", sensorReading.getBatteryLevel());

                        if (LocalBroadcastManager.getInstance(RTDemoService.getContext()).sendBroadcast(batteryUpdate)) {
                            Log.d(TAG, "Send EnliteSensor broadcast to update the UI");
                        } else {
                            Log.d(TAG, "Unable to send EnliteSensor broadcast to UI");
                        }
                    }
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
                double nearestSensorReading = 0;

                for (SensorMeasurement measurement : this.lastSensorReadings) {
                    long diff = glucoseReading.createdDifference(measurement.getCreated());
                    if (diff < secondsDifference) {
                        secondsDifference = diff;
                        nearestSensorReading = measurement.getIsig();
                    }
                }

                // Check if they are close enough
                if (secondsDifference < MedtronicReading.EXPIRATION_FOUR_MINUTES) {
                    this.knownCalibrations.add(new CalibrationPair(nearestSensorReading, glucoseReading.getMgdl()));
                }
            } else {
                // Different packages...
                /*
                                            Intent batteryUpdate = new Intent(Intents.RILEYLINK_BATTERY_UPDATE);
                            batteryUpdate.putExtra("battery", characteristic[0]);

                            LocalBroadcastManager.getInstance(context).sendBroadcast(batteryUpdate);
                 */
            }
        }
    }

    public Queue<MeterReading> getClucose() {
        return this.lastGlucoseReadings;
    }

    public List<CalibrationPair> getCalibrationPairs() {
        return this.knownCalibrations;
    }

}
