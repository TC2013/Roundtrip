package com.gxwtech.rtdemo.Medtronic.PumpData;

/**
 * Created by geoff on 5/29/15.
 *
 * Just need a class to keep the pair together, for parcel transport.
 */
public class TempBasalPair {
    public TempBasalPair() { mInsulinRate = 0.0; mDurationMinutes = 0;}
    public TempBasalPair(double insulinRate, int durationMinutes) {
        mInsulinRate = insulinRate;
        mDurationMinutes = durationMinutes;
    }
    public double mInsulinRate;
    public int mDurationMinutes;
}
