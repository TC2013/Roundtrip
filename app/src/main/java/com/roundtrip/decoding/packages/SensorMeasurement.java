package com.roundtrip.decoding.packages;

import android.support.annotation.NonNull;

import com.roundtrip.decoding.GlucosUnitConversion;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

public class SensorMeasurement implements Comparator<SensorMeasurement>, Comparable<SensorMeasurement>  {
    private final Date created;
    private final double isig;

    public SensorMeasurement(Date created, double isig) {
        this.created = created;
        this.isig = isig;
    }

    public Date getCreated() {
        return this.created;
    }

    public double getIsig(){
        return isig;
    }

    @Override
    public int compareTo(@NonNull SensorMeasurement sensorReading) {
        return this.compare(this, sensorReading);
    }

    @Override
    public boolean equals(@NonNull Object medtronicSensor) {
        return medtronicSensor instanceof SensorReading && this.compareTo((SensorMeasurement) medtronicSensor) == 0;
    }

    @Override
    public int compare(SensorMeasurement glucoseOne, SensorMeasurement glucoseTwo) {
        Long t1 = glucoseOne.getCreated().getTime();
        Long t2 = glucoseTwo.getCreated().getTime();

        // If they are within four minutes, and the same value, they must be the same :)
        if (Math.abs(t1 - t2) < MedtronicReading.EXPIRATION_MINUTE) {
            return 0;
        }

        return t1.compareTo(t2);
    }

    @Override
    public String toString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        return "Sensor ISIG " + this.isig + ", at " + df.format(this.getCreated());
    }
}
