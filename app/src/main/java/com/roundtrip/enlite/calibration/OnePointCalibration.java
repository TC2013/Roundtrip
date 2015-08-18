package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import java.util.Set;

public class OnePointCalibration implements CalibrationAlgorithm {
    @Override
    public double approximateGlucoseLevel(SensorReading sensorMeasurement, Set<CalibrationPair> calibrationPoints) {
        CalibrationPair calibrationPoint = calibrationPoints.iterator().next();

        double m = calibrationPoint.getSensorReading().getIsig() /
                calibrationPoint.getMeterReading().getMgdl();

        return sensorMeasurement.getIsig() / m;
    }
}
