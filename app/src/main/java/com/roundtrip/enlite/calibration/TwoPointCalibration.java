package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TwoPointCalibration implements CalibrationAlgorithm {
    @Override
    public double approximateGlucoseLevel(double sensorMeasurement, List<CalibrationPair> calibrationPoints) {

        Iterator<CalibrationPair> iter = calibrationPoints.iterator();

        CalibrationPair firstCalibrationPoint = iter.next();
        CalibrationPair secondCalibrationPoint = iter.next();

        double m1 = (firstCalibrationPoint.getSensorReading() - secondCalibrationPoint.getSensorReading());
        double m2 = (firstCalibrationPoint.getMeterReading() - secondCalibrationPoint.getMeterReading());

        double m =  m1 / m2;

        double b = secondCalibrationPoint.getSensorReading() - (m * secondCalibrationPoint.getMeterReading());

        return (sensorMeasurement - b) / m;
    }
}
