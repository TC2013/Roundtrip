package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class ChangeUtility extends TimeStampedRecord {

    public ChangeUtility() {
        super();
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
}
