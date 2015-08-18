package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.SensorReading;

import java.util.Set;

public class TwoPointCalibration implements CalibrationAlgorithm {
    @Override
    public double approximateGlucoseLevel(SensorReading sensorMeasurement, Set<CalibrationPair> calibrationPoints) {
        CalibrationPair firstCalibrationPoint = calibrationPoints.iterator().next();
        CalibrationPair secondCalibrationPoint = calibrationPoints.iterator().next();

        double m = (firstCalibrationPoint.getSensorReading().getIsig() -
                secondCalibrationPoint.getSensorReading().getIsig()) /
                (firstCalibrationPoint.getMeterReading().getMgdl() -
                        secondCalibrationPoint.getMeterReading().getMgdl());

        double b = secondCalibrationPoint.getSensorReading().getIsig() - (m * secondCalibrationPoint.getMeterReading().getMgdl());

        return (sensorMeasurement.getIsig() - b) / m;
    }
}
