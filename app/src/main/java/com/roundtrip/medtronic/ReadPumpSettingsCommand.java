package com.roundtrip.medtronic;

import com.roundtrip.medtronic.PumpData.PumpSettings;

public class ReadPumpSettingsCommand extends MedtronicCommand {
    protected PumpSettings mPumpSettings;

    public ReadPumpSettingsCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_PUMP_SETTINGS);
        // settings for running the command
        mNRetries = 2;
        mMaxRecords = 1;
        mSleepForPumpResponse = 5000;
        mSleepForPumpRetry = 501;
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
