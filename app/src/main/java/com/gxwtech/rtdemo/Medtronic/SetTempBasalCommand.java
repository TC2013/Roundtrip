package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by Geoff on 5/2/2015.
 */
public class SetTempBasalCommand extends MedtronicCommand {
    protected int mInsulinRate;
    protected int mDurationMinutes;
    SetTempBasalCommand(int insulinRate, int durationMinutes) {
        init(MedtronicCommandEnum.CMD_M_TEMP_BASAL_RATE);
        mInsulinRate = insulinRate;
        mDurationMinutes = durationMinutes;
        mNRetries = 0;
    }
}
