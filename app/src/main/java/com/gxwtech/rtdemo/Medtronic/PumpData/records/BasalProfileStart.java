package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class BasalProfileStart extends TimeStampedRecord {
    private static final String TAG = "BasalProfileStart";
    private int offset;
    private float rate;
    private int index;

    public BasalProfileStart() {
        bodySize = 3;
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        super.collectRawData(data, model);
        decode(data);
        return true;
    }

    public boolean decode(byte[] data) {
        if (!super.decode(data)) {
            return false;
        }
        int bodyOffset = headerSize + timestampSize;
        offset = data[bodyOffset] * 1000 * 30 * 60;
        if (model == PumpModel.MM523) {
            rate = data[bodyOffset + 1] * 0.025f;
        }
        index = data[bodyOffset + 2];
        logRecord();
        return true;
    }

    @Override
    public void logRecord() {
        Log.i(TAG, String.format("%s %s Offset: %02f Rate: %02f",
                timeStamp.toString(), recordTypeName, offset, rate));
    }
}
