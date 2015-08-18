package com.roundtrip.medtronic;

public class BolusCommand extends MedtronicCommand {
    public BolusCommand() {
        init(MedtronicCommandEnum.CMD_M_BOLUS);
    }
}
