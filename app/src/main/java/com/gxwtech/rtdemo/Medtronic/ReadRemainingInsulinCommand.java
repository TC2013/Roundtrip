package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadRemainingInsulinCommand extends MedtronicCommand {
    public ReadRemainingInsulinCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_INSULIN_REMAINING);
    }
}
