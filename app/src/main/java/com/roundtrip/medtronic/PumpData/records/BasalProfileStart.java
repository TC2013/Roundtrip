package com.roundtrip.medtronic.PumpData.records;

import android.util.Log;

import com.roundtrip.medtronic.PumpModel;

// todo: rewrite with new knowledge of basal profiles (see BasalProfile.java)
public class BasalProfileStart extends TimeStampedRecord {
    private static final String TAG = "BasalProfileStart";
    private int offset;
    private float rate;
    private int index;

    public BasalProfileStart() {
        super();

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
        offset = readUnsignedByte(data[bodyOffset]) * 1000 * 30 * 60;
        if (model == PumpModel.MM523) {
            rate = readUnsignedByte(data[bodyOffset + 1]) * 0.025f;
        }
        index = readUnsignedByte(data[bodyOffset + 2]);
        //logRecord();
        return true;
    }

    @Override
    public void logRecord() {
        Log.i(TAG, String.format("%s %s Offset: %d Rate: %.2f",
                timeStamp.toString(), recordTypeName, offset, rate));
    }
}
