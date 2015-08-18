package com.roundtrip.medtronic.PumpData.records;

import com.roundtrip.medtronic.PumpModel;

public class NoDeliveryAlarm extends TimeStampedRecord {
    public NoDeliveryAlarm() {
        super();

        headerSize = 4;
        calcSize();
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }
}
