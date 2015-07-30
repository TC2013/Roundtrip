package com.gxwtech.rtdemo.medtronic.PumpData.records;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class Sara6E extends Record {
    public Sara6E() {
        headerSize = 1;
        timestampSize = 2;
        bodySize = 49;
        calcSize();
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        return super.collectRawData(data, model);
    }

    @Override
    protected boolean decode(byte[] data) {
        return true;
    }
}
