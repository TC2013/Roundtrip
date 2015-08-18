package com.roundtrip.enlite.calibration;

import com.roundtrip.decoding.packages.MeterReading;
import com.roundtrip.decoding.packages.SensorReading;

public class CalibrationPair {
    private final SensorReading sensorReading;
    private final MeterReading meterReading;

    public CalibrationPair(SensorReading sensorReading, MeterReading meterReading) {
        this.sensorReading = sensorReading;
        this.meterReading = meterReading;
    }

    public SensorReading getSensorReading(){
        return this.sensorReading;
    }

    public MeterReading getMeterReading() {
        return this.meterReading;
    }
}
