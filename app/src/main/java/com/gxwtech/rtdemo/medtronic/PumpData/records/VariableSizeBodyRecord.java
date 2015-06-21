package com.gxwtech.rtdemo.medtronic.PumpData.records;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class VariableSizeBodyRecord extends Record {

    public VariableSizeBodyRecord() {
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return true;
    }

    protected boolean decode(byte[] data) {
        return true;
    }
}
