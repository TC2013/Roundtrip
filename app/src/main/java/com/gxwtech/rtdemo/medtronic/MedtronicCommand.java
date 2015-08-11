package com.gxwtech.rtdemo.medtronic;

import android.util.Log;

/**
 * Created by Geoff on 4/27/15.
 */
public class MedtronicCommand {
    private static final String TAG = "MedtronicCommand";
    private static final boolean DEBUG_MEDTRONICCOMMAND = false;
    protected MedtronicCommandEnum mCode;
    protected MedtronicCommandStatusEnum mStatus;
    protected byte[] mPacket;
    protected byte[] mParams; // may be null!

    // button is zero, unless command 93 (SET_POWER_CONTROL) in which case, 85.
    // No, we don't know why :(
    protected byte mButton = 0;
    // +++ what's all this talk of a new way?
    protected byte[] mRawReceivedData;
    protected int mSleepForPumpResponse = 100;
    protected int mSleepForPumpRetry = 500; //millis
    byte mNRetries = 2;
    byte mBytesPerRecord = 64;
    byte mMaxRecords = 1;

    public MedtronicCommand() {
        init();
    }

    protected void init() {
        mCode = MedtronicCommandEnum.CMD_M_INVALID_CMD;
        mPacket = null;
        mParams = null;
        mButton = 0;
    }

    protected void init(MedtronicCommandEnum which) {
        init();
        mCode = which;
    }

    public MedtronicCommandEnum getCode() {
        return mCode;
    }

    public String getName() {
        return mCode.toString();
    }

    public byte calcRecordsRequired() {
        byte rval;
        int len = mBytesPerRecord * mMaxRecords;
        int i = len / 64;
        int j = len % 64;
        if (j > 0) {
            rval = (byte) (i + 1);
        } else {
            rval = (byte) i;
        }
        return rval;
    }

    // TODO: figure out how to get notification up to the gui that we're sleeping.
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // subclasses should override parse() and get data from mMResponse
    protected void parse(byte[] receivedData) {
        Log.w(TAG, "Base class parse called on command " + getName());
    }

    public MedtronicCommandStatusEnum run() {
        // Rewrite for bluetooth

        return null;
    }

}
