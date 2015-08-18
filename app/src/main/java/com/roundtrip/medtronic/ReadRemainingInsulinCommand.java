package com.roundtrip.medtronic;

public class ReadRemainingInsulinCommand extends MedtronicCommand {
    public ReadRemainingInsulinCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_INSULIN_REMAINING);
    }
}
