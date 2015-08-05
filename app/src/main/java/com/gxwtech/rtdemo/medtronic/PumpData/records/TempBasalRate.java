package com.gxwtech.rtdemo.medtronic.PumpData.records;

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
            // this is the second byte of the packet (i.e. header), not the body.
            basalRate = readUnsignedByte(data[1]) * 0.025;
        }
        return true;
    }


}
