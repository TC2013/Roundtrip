package com.gxwtech.rtdemo.Medtronic.PumpData.records;


import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

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
        programmedAmount = data[1] / 10.0f;
        deliveredAmount = data[2] / 10.0f;
        if (model == PumpModel.MM523) {
            programmedAmount = data[2] / 40.0f;
            deliveredAmount = data[4] / 40.0f;
            unabsorbed = data[6] / 40.0f;
            duration = data[7] * 30;
        } else {
            programmedAmount = data[1] / 10.0f;
            deliveredAmount = data[2] / 10.0f;
            duration = data[3] * 30;
        }
        bolusType = (duration > 0) ? BolusType.SQUARE : BolusType.NORMAL;
        Log.e(TAG,"SUCCESS! Parsed Bolus Record");
        logRecord();
        return true;
    }

    public enum BolusType {
        SQUARE,
        NORMAL
    }

    @Override
    public void logRecord() {
        Log.i(TAG, String.format("%s %s Programmed amount: %02f Delivered: %02f Duration: %02f Type: %s Unabsorbed: %2f",
                timeStamp, recordTypeName, programmedAmount, deliveredAmount, duration, bolusType.name(), unabsorbed));
    }
}
