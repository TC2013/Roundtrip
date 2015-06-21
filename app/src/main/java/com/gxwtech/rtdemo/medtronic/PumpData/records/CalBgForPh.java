package com.gxwtech.rtdemo.medtronic.PumpData.records;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class CalBgForPh extends TimeStampedRecord {
    public CalBgForPh() {
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
}
