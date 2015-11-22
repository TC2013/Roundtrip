package com.gxwtech.rtdemo.medtronic;

/**
 * Created by geoff on 4/27/15.
 */
public class WakePumpCommand extends MedtronicCommand {
    public WakePumpCommand() {
        init(MedtronicCommandEnum.CMD_M_POWER_CTRL);
        mParams = new byte[] {0x00};
    }
}
