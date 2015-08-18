package com.roundtrip.medtronic.PumpData;

/**
 * <p/>
 * Just need a class to keep the pair together, for parcel transport.
 */
public class TempBasalPair {
    public double mInsulinRate;
    public int mDurationMinutes;

    public TempBasalPair() {
        mInsulinRate = 0.0;
        mDurationMinutes = 0;
    }

    public TempBasalPair(double insulinRate, int durationMinutes) {
        mInsulinRate = insulinRate;
        mDurationMinutes = durationMinutes;
    }
}
