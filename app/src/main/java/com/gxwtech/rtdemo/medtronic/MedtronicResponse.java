package com.gxwtech.rtdemo.medtronic;

import com.gxwtech.RileyLink.RileyLinkCommandResult;
import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.CRC;

/**
 * Created by geoff on 5/5/15.
 * This class overlaps Carelink and Medtronic.  Refactor.
 * This class exists to keep all the "explanation" and parsing of the packet
 * in one place, rather than spread out over layers of code.
 */

public class MedtronicResponse extends RileyLinkCommandResult {
    public MedtronicResponse() {
        super();
    }

    public void parseFrom(byte[] rawdata) {
        mPacket = rawdata;
        mStatus = MedtronicResponse.STATUS_OK;
        mStatusMessage = "Response received";
    }

    /*
Receive from carelink's radio buffer:
    0x00000000 02 00 01 00 DD 80 01 A7 01 46 73 24 55 00 00
    02 00 01: radio buffer ok?
    00 DD: ?
    80 01: one byte response, end of data?
    A7 01: ("Response to transmitted packet"?)
    46 73 24: pump serial number
    55: checksum of first 12 bytes
    00: one parameter
    00: checksum of one parameter?
    */

}
