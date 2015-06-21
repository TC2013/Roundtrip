package com.gxwtech.rtdemo.carelink;

/**
 * Created by geoff on 4/28/15.
 */

// This reads the signal strength
public class SignalStrengthCommand extends CarelinkCommand {
    protected byte mSignalStrength = 0;
    public SignalStrengthCommand() {
        init(CarelinkCommandEnum.CMD_C_SIGNAL_STRENGTH);
    }
    // promote mSignalStrength to int, makes it positive again for values > 127
    public int getSignalStrength() {
        int rval = mSignalStrength;
        if (rval < 0) {
            rval = rval + 256;
        }
        return rval;
    }

    protected void parse() {
        // Note: only the one byte is sent back
        // byte[3], meaning the total packet is 4 bytes
        byte[] response = getRawResponse();
        if (response != null) {
            if (response.length > 3) {
                mSignalStrength = response[3];
            }
        }
    }
}
