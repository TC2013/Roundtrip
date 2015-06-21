package com.gxwtech.rtdemo.medtronic;

/**
 * Created by Geoff on 5/2/2015.
 *
 * double insulinRate: insulin in units (granularity 0.025 U)
 * int durationMinutes: duration in minutes (granularity 30min)
 *
 * Can Cancel a running temp basal with a delivery of 0 units, 0 duration.
 * Can run a temp of zero units for a given period of time to temporarily
 *   decrease insulin delivery from a normal basal.
 *
 * from mm-tempbasals.py:
 *def format_params (args):
 *   duration = args.duration / 30
 *   rate = int(args.rate / 0.025)
 *   params = [0x00, rate, duration]
 *   return params
 */

public class SetTempBasalCommand extends MedtronicCommand {
    protected boolean mParamsOk = false;
    public SetTempBasalCommand(double insulinRate, int durationMinutes) {
        init(MedtronicCommandEnum.CMD_M_TEMP_BASAL_RATE);
        mNRetries = 1;
        mMaxRecords = 0; //?
        // maximum would be 255 * 0.025 == 6.375 which is above the maximum of the pump.
        mParams = new byte[3];
        mParams[0] = 0x00;
        // sets mParams[1], mParams[2]:
        if (setInsulinRate(insulinRate) && setDurationMinutes(durationMinutes)) {
            mParamsOk = true;
        }
    }

    public boolean setInsulinRate(double rate) {
        boolean rval = true;
        // fixme: fix hardcoded maximum?
        if (rate > 3.0) {
            rate = 3.0;
            rval = false;
        }
        if (rate < 0) {
            rate = 0;
            rval = false;
        }
        byte insulinRateByte = (byte)(Math.floor(rate / 0.025));
        mParams[1] = insulinRateByte;
        return rval;
    }

    public double getInsulinRate() {
        return ((int)(mParams[1])) * 0.025;
    }

    public boolean setDurationMinutes(int minutes) {
        boolean rval = true;
        if (minutes < 0) {
            minutes = 0;
            rval = false;
        }
        // I'm not sure what the pump's maximum is here, use 24 hours
        final int minutes_max = 2 * 24 * 30;
        if (minutes > minutes_max) {
            minutes = minutes_max;
            rval = false;
        }
        byte durationByte = (byte)(Math.floor(minutes / 30));
        mParams[2] = durationByte;
        return rval;
    }

    public int getDurationMinutes() {
        return mParams[2] * 30;
    }


}
