package com.gxwtech.rtdemo.medtronic.PumpData.records;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class SelectBasalProfile extends TimeStampedRecord {

    public SelectBasalProfile() {
        calcSize();
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
}
