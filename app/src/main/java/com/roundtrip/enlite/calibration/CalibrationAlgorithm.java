package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import java.util.List;
import java.util.Set;

public interface CalibrationAlgorithm {
    public double approximateGlucoseLevel(double sensorMeasurement, List<CalibrationPair> calibrationPoints);
}
