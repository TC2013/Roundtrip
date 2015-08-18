package com.roundtrip.medtronic;

public class ReadPumpStatusCommand extends MedtronicCommand {
    public ReadPumpStatusCommand() {
        // how is this different from read_pump_state?
        init(MedtronicCommandEnum.CMD_M_READ_PUMP_STATUS);
    }
}
