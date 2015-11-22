package com.gxwtech.rtdemo.medtronic;

import com.gxwtech.RileyLink.RileyLinkCommandResult;
import com.gxwtech.RileyLink.RileyLinkUtil;
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
    /* todo: much of this should go in the superclass */
    public byte[] mRawdata;
    public byte rssi;           // part of RileyLink, not part of medtronic
    public byte sequenceNumber; // part of RileyLink, not part of medtronic
    public byte receivedChecksum;       // part of RileyLink, not part of medtronic
    public MedtronicResponse() {
        super();
    }

    public void parseFrom(byte[] rawdata) {
        /* TODO: this needs to weed out the RSSI, sequence and checksum bytes */
        mRawdata = rawdata;
        mPacket = null;
        if (rawdata == null) {
            mStatus = MedtronicResponse.STATUS_ERROR;
            mStatusMessage = "Received null data";
        } else {
            if (rawdata.length <=3 ) {
                mStatus = MedtronicResponse.STATUS_ERROR;
                mStatusMessage = "Received short message";
            } else {
                /* todo: do more checking (checksum, sequence number, etc.) */
                rssi = rawdata[0];
                sequenceNumber = rawdata[1];
                receivedChecksum = rawdata[rawdata.length-1];
                mPacket = new byte[rawdata.length-3];
                System.arraycopy(rawdata,2,mPacket,0,rawdata.length-3);
                byte calculatedChecksum = CRC.computeCRC(mPacket);
                if (calculatedChecksum != receivedChecksum) {
                    mStatus = MedtronicResponse.STATUS_CRC_ERROR;
                    mStatusMessage = "CRC mismatch on packet";
                } else {
                    mStatus = MedtronicResponse.STATUS_OK;
                    mStatusMessage = "Response received";
                }
            }
        }
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
