package com.gxwtech.rtdemo.medtronic.PumpData.records;


import android.util.Log;

import com.gxwtech.rtdemo.medtronic.PumpModel;

public class Bolus extends TimeStampedRecord {
    private final static String TAG = "Bolus";
    private float programmedAmount;
    private float deliveredAmount;
    private float duration;
    private float unabsorbed = -1;
    private BolusType bolusType;


    public Bolus() {
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (model == PumpModel.MM523) {
            headerSize = 8;
        } else {
            headerSize = 4;
        }
        calcSize();
        if (!super.collectRawData(data, model)) {
            return false;
        }
        return decode(data);
    }

    protected boolean decode(byte[] data) {
        if (!super.decode(data)) {
            return false;
        }
        programmedAmount = readUnsignedByte(data[1]) / 10.0f;
        deliveredAmount = readUnsignedByte(data[2]) / 10.0f;
        if (model == PumpModel.MM523) {
            programmedAmount = readUnsignedByte(data[2]) / 40.0f;
            deliveredAmount = readUnsignedByte(data[4]) / 40.0f;
            unabsorbed = readUnsignedByte(data[6]) / 40.0f;
            duration = readUnsignedByte(data[7]) * 30;
        } else {
            programmedAmount = readUnsignedByte(data[1]) / 10.0f;
            deliveredAmount = readUnsignedByte(data[2]) / 10.0f;
            duration = readUnsignedByte(data[3]) * 30;
        }
        bolusType = (duration > 0) ? BolusType.SQUARE : BolusType.NORMAL;
        //logRecord();
        return true;
    }

    public enum BolusType {
        SQUARE,
        NORMAL
    }

    @Override
    public void logRecord() {
        Log.i(TAG, String.format("%s %s Programmed amount: %.2f Delivered: %.2f Duration: %.2f Type: %s Unabsorbed: %.2f",
                timeStamp, recordTypeName, programmedAmount, deliveredAmount, duration, bolusType.name(), unabsorbed));
    }
}
