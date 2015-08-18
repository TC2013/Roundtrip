package com.roundtrip.medtronic;

import android.util.Log;

import com.roundtrip.medtronic.PumpData.TempBasalPair;

public class ReadBasalTempCommand extends MedtronicCommand {
    private static final String TAG = "ReadBasalTempCommand";
    protected TempBasalPair mTempBasalPair;

    public ReadBasalTempCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_TEMP_BASAL);
        mMaxRecords = 1;
        mTempBasalPair = new TempBasalPair();
    }

    protected static int readUnsignedByte(byte b) {
        return (b < 0) ? b + 256 : b;
    }

    protected void parse(byte[] receivedData) {
        //Log.e(TAG,"parse: here is the data:" + HexDump.dumpHexString(receivedData));
        // cache result in mTempBasalPair

        // Dunno why, but the response format is:
        // 0x00, 0x00, 0x00, (temp basal triplet)
        // 30 minutes, 0.25 U: 00 00 00 0A 00 1E
        // 1 hour, 0.35 U:     00 00 00 0E 00 3C
        // 24 hours, 0.40 U:   00 00 00 10 05 A0
        if (receivedData == null) {
            Log.e(TAG, "parse: null data");
            return;
        }
        if (receivedData.length < 6) {
            Log.e(TAG, "parse: receivedData buffer too small");
            return;
        }

        int rateByte = readUnsignedByte(receivedData[3]);
        int durationHighByte = readUnsignedByte(receivedData[4]);
        int durationLowByte = readUnsignedByte(receivedData[5]);
        int minutes = (durationHighByte * 256) + durationLowByte;

        mTempBasalPair.mInsulinRate = rateByte * 0.025;
        mTempBasalPair.mDurationMinutes = minutes;
        Log.v(TAG, String.format("TempBasalPair read as: insulinRate: %.3f U, duration %d minutes",
                mTempBasalPair.mInsulinRate, mTempBasalPair.mDurationMinutes));
    }

    public TempBasalPair getTempBasalPair() {
        return mTempBasalPair;
    }
    /*
    public PumpSettings getPumpSettings() {
        return mPumpSettings;
    }
    */
}
