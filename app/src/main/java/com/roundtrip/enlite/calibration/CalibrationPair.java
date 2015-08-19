package com.roundtrip.enlite.calibration;

public class CalibrationPair {
    private final double sensorReading;
    private final double meterReading;

    public CalibrationPair(double sensorReading, double meterReading) {
        this.sensorReading = sensorReading;
        this.meterReading = meterReading;
    }

    public double getSensorReading() {
        return this.sensorReading;
    }

    public double getMeterReading() {
        return this.meterReading;
    }
}
