package com.roundtrip.enlite.calibration;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;

public class LinearRegressionCalibration implements CalibrationAlgorithm {
    @Override
    public double approximateGlucoseLevel(double sensorMeasurement, List<CalibrationPair> calibrationPoints) {
        SimpleRegression regression = new SimpleRegression();

        for (CalibrationPair point : calibrationPoints) {
            regression.addData(point.getSensorReading(), point.getMeterReading());
        }

        return regression.predict(sensorMeasurement);
    }
}
