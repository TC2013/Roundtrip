package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class UnabsorbedInsulin extends VariableSizeBodyRecord {

    public UnabsorbedInsulin() {
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        bodySize = data[1];
        calcSize();
        return true;
    }
}
