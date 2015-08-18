package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class ChangeRemoteId extends TimeStampedRecord {

    public ChangeRemoteId() {
        super();
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return this.decode(data);
    }
}
