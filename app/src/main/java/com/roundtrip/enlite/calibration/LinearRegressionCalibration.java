package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;
import java.util.Set;

public class LinearRegressionCalibration implements CalibrationAlgorithm {
    @Override
    public double approximateGlucoseLevel(double sensorMeasurement, List<CalibrationPair> calibrationPoints) {
        SimpleRegression regression = new SimpleRegression();

        for (CalibrationPair point : calibrationPoints) {
            regression.addData(point.getSensorReading(),point.getMeterReading());
        }

        return regression.predict(sensorMeasurement);
    }
}
