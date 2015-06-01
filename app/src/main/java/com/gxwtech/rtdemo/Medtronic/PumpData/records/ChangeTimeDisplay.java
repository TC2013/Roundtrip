package com.gxwtech.rtdemo.Medtronic.PumpData.records;


import com.gxwtech.rtdemo.Medtronic.PumpModel;

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
