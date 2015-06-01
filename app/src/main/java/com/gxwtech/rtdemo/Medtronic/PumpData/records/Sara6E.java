package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class Sara6E extends Record {
    public Sara6E() {
        headerSize = 1;
        timestampSize = 2;
        bodySize = 49;
        calcSize();
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean decode(byte[] data) {
        return true;
    }
}
