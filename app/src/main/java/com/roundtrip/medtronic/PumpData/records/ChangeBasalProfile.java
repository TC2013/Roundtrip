package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class ChangeBasalProfile extends TimeStampedRecord {
    public ChangeBasalProfile() {
        super();
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
