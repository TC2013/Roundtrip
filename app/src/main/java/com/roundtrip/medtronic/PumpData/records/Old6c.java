package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class Old6c extends Record {
    public Old6c() {
        super();

        bodySize = 38;
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean decode(byte[] data) {
        return true;
    }
}
