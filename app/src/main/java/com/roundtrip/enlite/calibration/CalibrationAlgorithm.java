package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import java.util.Set;

public interface CalibrationAlgorithm {
    public double approximateGlucoseLevel(SensorReading sensorMeasurement, Set<CalibrationPair> calibrationPoints);
}
