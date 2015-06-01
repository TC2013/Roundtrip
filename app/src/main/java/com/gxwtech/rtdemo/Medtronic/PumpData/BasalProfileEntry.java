package com.gxwtech.rtdemo.Medtronic.PumpData;

import org.joda.time.LocalTime;

/**
 * Created by geoff on 6/1/15.
 * This is a helper class for BasalProfile, only used for interpreting the contents of BasalProfile
 */
public class BasalProfileEntry {
    public double rate;
    public LocalTime startTime;
    public BasalProfileEntry() {
        rate = 0.0;
        startTime = new LocalTime(0,0);
    }
    public BasalProfileEntry(int rateByte, int startTimeByte) {
        // rateByte is insulin delivery rate, U/hr, in 0.025 U increments
        // startTimeByte is time-of-day, in 30 minute increments
        rate = rateByte * 0.025;
        startTime = new LocalTime(startTimeByte / 2, (startTimeByte % 2) * 30);
    }
}
