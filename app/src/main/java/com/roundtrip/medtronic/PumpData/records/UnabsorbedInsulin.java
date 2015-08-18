package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class UnabsorbedInsulin extends VariableSizeBodyRecord {

    public UnabsorbedInsulin() {
        super();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        bodySize = readUnsignedByte(data[1]);
        calcSize();
        return true;
    }
}
