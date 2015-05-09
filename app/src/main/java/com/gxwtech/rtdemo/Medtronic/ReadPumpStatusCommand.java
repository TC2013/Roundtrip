package com.gxwtech.rtdemo.Medtronic;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadPumpStatusCommand extends MedtronicCommand {
    public ReadPumpStatusCommand() {
        // how is this different from read_pump_state?
        init(MedtronicCommandEnum.CMD_M_READ_PUMP_STATUS);
    }
}
