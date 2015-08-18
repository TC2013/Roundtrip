package com.roundtrip.medtronic;

public class KeypadPushCommand extends MedtronicCommand {
    // todo: turn this into a proper enum
    public byte keypress_act = 0x02;
    public byte keypress_down = 0x04;
    public byte keypress_up = 0x03;
    public byte keypress_esc = 0x01;
    public byte keypress_easy = 0x00;
    public byte mKey;

    public void KeypadPushCommand(byte key) {
        mKey = key;
        init(MedtronicCommandEnum.CMD_M_KEYPAD_PUSH);
        mNRetries = 1;
        mMaxRecords = 0;
    }
}
