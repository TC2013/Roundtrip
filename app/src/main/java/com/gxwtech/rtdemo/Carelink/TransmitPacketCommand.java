package com.gxwtech.rtdemo.Carelink;

import com.gxwtech.rtdemo.Carelink.util.ByteUtil;
import com.gxwtech.rtdemo.Carelink.util.CRC8;

/**
 * Created by geoff on 4/28/15.
 */
public class TransmitPacketCommand extends CarelinkCommand {
    byte[] mSerialNumber;
    byte mCode;
    byte[] mParams;
    byte mButton;
    byte mNRetries;
    byte mNRecords;

    /*
     * Send a packet to the Medtronic Minimed.
     * code is the command code (See MedtronicCommandEnum)
     * params is an array of the parameters specific to the command
     * button is zero, unless command 93 (power_control) in which case it is 85 (yuck)
     * nRetries is ? (try 2 or 5)
     * nRecords is zero or one: bytesPerRecord * maxRecords / 64, round up.
     * (I think this is because both minimed and carelink want to use 64 byte packets,
     * so we're transferring 64 byte packets over a 64 byte packet link...)
     *
     */
    public TransmitPacketCommand(byte code,
                                 byte[] params,
                                 byte[] serialNumber,
                                 byte button,
                                 byte nRetries,
                                 byte nRecords) {

        init(CarelinkCommandEnum.CMD_C_TRANSMIT_PACKET);
        setSerialNumber(serialNumber);
        mCode = code;
        mParams = params;
        mButton = button;
        mNRetries = nRetries;
        mNRecords = nRecords;
    }

    public void setSerialNumber(byte[] serialNumber) {
        mSerialNumber = serialNumber;
    }

    // this code could be optimized for speed, but why?
    // the delay is all in the communication with the pump...
    protected byte[] preparePacket() {
        byte[] rval;
        // We completely override preparePacket for this command
        // Refactor to fix?
        byte[] head = {1,0,(byte)167, 1};
        rval = ByteUtil.concat(head, mSerialNumber);
        // ugly...
        short nParams = 0;
        if (mParams != null) {
            nParams = (short)mParams.length;
        }
        // change them to bytes
        byte lb = (byte)((nParams % 256) & 0xFF);
        // why set high bit? cause that's what Carelink wants...
        byte hb = (byte)((nParams / 256) | 0x80);
        byte[] payload = new byte[7];
        payload[0] = hb;
        payload[1] = lb;

        payload[2] = mButton;
        payload[3] = mNRetries;
        payload[4] = mNRecords;
        payload[5] = 0;
        payload[6] = mCode;
        // add the payload to the packet
        rval = ByteUtil.concat(rval,payload);
        // compute CRC for what we've got so far
        byte payloadCRC = CRC8.crc8(rval);
        // add it to the packet
        rval = ByteUtil.concat(rval,payloadCRC);
        if (nParams > 0) {
            // add the parameters
            rval = ByteUtil.concat(rval, mParams);
            // compute CRC for parameters
            byte paramsCRC = CRC8.crc8(mParams);
            // add the CRC for the parameters
            rval = ByteUtil.concat(rval, paramsCRC);
        }

        return rval;
    }

}
