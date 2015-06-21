package com.gxwtech.rtdemo.medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class TempBasalRate extends TimeStampedRecord {
    private static final String TAG = "TempBasalRate";
    public double basalRate; // rate in Units/hr
    public TempBasalRate() {
        calcSize();
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
    protected boolean decode(byte[] data) {
        if (!super.decode(data)) {
            return false;
        }
        int bodyIndex = headerSize + timestampSize;
        if (data.length > bodyIndex) {
            byte basalRateByte = data[1]; // this is the second byte of the packet, not the body.
            basalRate = basalRateByte * 0.025;
            Log.d(TAG, "SUCCESS! Parsed TempBasalRate Record");
        }
        return true;
    }


}
