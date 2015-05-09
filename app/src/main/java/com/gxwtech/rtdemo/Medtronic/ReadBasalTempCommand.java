package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadBasalTempCommand extends MedtronicCommand {
    public ReadBasalTempCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_TEMP_BASAL);
    }
}
