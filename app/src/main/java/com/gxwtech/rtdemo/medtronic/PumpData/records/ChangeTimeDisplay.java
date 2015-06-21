package com.gxwtech.rtdemo.medtronic.PumpData.records;


import com.gxwtech.rtdemo.medtronic.PumpModel;

public class ChangeTimeDisplay extends TimeStampedRecord {

    public ChangeTimeDisplay() {
        calcSize();
    }
    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
}
