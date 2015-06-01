package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class EndResultsTotals extends Record {

    public EndResultsTotals() {
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        headerSize = 5;
        timestampSize = 2;
        bodySize = 3;
        calcSize();
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }

    protected boolean decode(byte[] data) {
        return true;
    }
}
