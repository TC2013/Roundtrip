package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class Prime extends TimeStampedRecord {
    private final static String TAG = "Prime";

    private float amount;
    private float fixed;
    private PrimeType primeType;

    public Prime() {
        headerSize = 5;
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }

    @Override
    protected boolean decode(byte[] data) {
        if (!super.decode(data)) {
            return false;
        }
        amount = data[4] / 10.0f;
        fixed = data[2] / 10.0f;
        primeType = (fixed == 0) ? PrimeType.MANUAL : PrimeType.FIXED;
        logRecord();
        return true;
    }

    private enum PrimeType {
        MANUAL,
        FIXED
    }

    @Override
    public void logRecord() {
        Log.i(TAG, String.format("%s %s Amount: %.2f Fixed: %.2f Type: %s",
                timeStamp, recordTypeName, amount, fixed, primeType.name()));
    }
}
