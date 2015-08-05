package com.gxwtech.rtdemo.medtronic.PumpData.records;

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
            // This is the second byte of the packet (i.e. header), not the body.
            durationMinutes = readUnsignedByte(data[1]) * 30;
        }
        return true;
    }

}
