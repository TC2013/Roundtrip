package com.gxwtech.rtdemo.medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class TempBasalDuration extends TimeStampedRecord {
    private static final String TAG = "TempBasalDuration";
    public int durationMinutes;
    public TempBasalDuration() {
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
            byte durationByte = data[1]; // This is the second byte of the packet, not the body.
            durationMinutes = durationByte * 30;
            Log.d(TAG, "SUCCESS! Parsed TempBasalDuration Record");
        }
        return true;
    }

}
