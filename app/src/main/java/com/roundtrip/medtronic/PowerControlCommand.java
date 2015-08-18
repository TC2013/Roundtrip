package com.roundtrip.medtronic;

// This is an example of what bewest's python decocare transmits:
// 01 00 a7 01
// 46 73 24
// 80 02 55 00 00 00 5d 17 01 0a a2

// what we sent:
// 01 00 A7 01
// 46 73 24
// 80 02 55 00 00 00 5D 17 01 0A A2

public class PowerControlCommand extends MedtronicCommand {

    public PowerControlCommand(byte onOff, byte minutes) {
        init(MedtronicCommandEnum.CMD_M_POWER_CTRL);
        mNRetries = 2;
        mButton = 85; // only command that requires this.  Why? dunno.
        mMaxRecords = 1; // one 1 byte record from pump.
        mParams = new byte[2];
        mParams[0] = onOff; // zero or one. One means turn on, zero means turn off
        mParams[1] = minutes; // how many minutes to turn on the radio power? try 3 or 10
        mSleepForPumpResponse = 17000; // OUCH!
    }

    // After we send the "transmit packet" request,
    // we read the link status.
    // here is what the link status is currently giving us:
    // 01 55 00 00 02 00 00 00 05 03 FE

    // here is what it should be (or something close to this):
    // 01 55 00 00 02 01 00 0f 05 04 00

    // in particular, I want to see the 0f
    // is it because my pump's battery is nearly gone?

    // get battery, try again
    // 01 55 00 00 02 00 00 00 05 03 FE
    // Nope, not the battery.

    // changed sleep(100) to sleep(1)
    // 01 55 00 00 02 00 00 00 05 03 FE


}
