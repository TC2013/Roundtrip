package com.gxwtech.rtdemo.Medtronic;

import com.gxwtech.rtdemo.Medtronic.PumpData.PumpSettings;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadPumpSettingsCommand extends MedtronicCommand {
    protected PumpSettings mPumpSettings;
    public ReadPumpSettingsCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_PUMP_SETTINGS);
        // settings for running the command
        mNRetries = 2;
        mMaxRecords = 1;
        mSleepForPumpResponse = 5000;
        mSleepForPumpRetry = 2000;
        // member variable initialization
        mPumpSettings = new PumpSettings();
    }
    protected void parse(byte[] receivedData) {
        mPumpSettings.parseFrom(receivedData);
    }
    public PumpSettings getPumpSettings() {
        return mPumpSettings;
    }
}
