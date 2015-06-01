package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

public class ChangeBasalProfile extends TimeStampedRecord {
    public ChangeBasalProfile() {
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        bodySize = 145;
        calcSize();
        return decode(data);
    }
}
