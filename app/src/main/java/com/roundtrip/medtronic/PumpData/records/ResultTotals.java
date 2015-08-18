package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class ResultTotals extends Record {
    private static final String TAG = "ResultTotals";

    public ResultTotals() {
        bodySize = 40;
        headerSize = 3; // 1 for opcode, 2 for a date stamp.
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }

    protected boolean decode(byte[] data) {
        return true;
    }


}
