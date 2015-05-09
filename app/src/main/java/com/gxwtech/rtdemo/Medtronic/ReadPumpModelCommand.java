package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by geoff on 5/4/15.
 */
public class ReadPumpModelCommand extends MedtronicCommand {
    static String TAG = "ReadPumpModelCommand";
    String mModel = "(empty model number)";
    public ReadPumpModelCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_PUMP_MODEL_NUMBER);
        mMaxRecords = 1; // one 64 byte record from pump
        mNRetries = 2;
        mSleepForPumpResponse = 5000;
    }

    protected void parse() {
        // todo: convert byte array to char string
    }
}
