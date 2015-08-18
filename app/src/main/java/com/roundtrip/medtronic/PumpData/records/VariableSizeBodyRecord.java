package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class VariableSizeBodyRecord extends Record {

    public VariableSizeBodyRecord() {
        super();
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
