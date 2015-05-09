package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadProfileCommand extends MedtronicCommand {
    public ReadProfileCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_B_PROFILES);
    }
}
