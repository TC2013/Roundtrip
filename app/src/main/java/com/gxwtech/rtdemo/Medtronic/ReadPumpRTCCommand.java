package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadPumpRTCCommand extends MedtronicCommand {
    public ReadPumpRTCCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_RTC);
        mNRetries = 2;
        mMaxRecords = 1;
    }
}
