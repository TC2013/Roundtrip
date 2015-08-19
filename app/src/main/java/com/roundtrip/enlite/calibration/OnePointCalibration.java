package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import java.util.List;
import java.util.Set;

public class OnePointCalibration implements CalibrationAlgorithm {
    @Override
    public double approximateGlucoseLevel(double sensorMeasurement, List<CalibrationPair> calibrationPoints) {
        CalibrationPair calibrationPoint = calibrationPoints.iterator().next();

        double m = calibrationPoint.getSensorReading() /
                calibrationPoint.getMeterReading();

        return sensorMeasurement / m;
    }
}
