package com.gxwtech.rtdemo.Medtronic;

import com.gxwtech.rtdemo.CRC;
import com.gxwtech.rtdemo.Carelink.util.ByteUtil;

/**
 * Created by geoff on 5/5/15.
 * This class overlaps Carelink and Medtronic.  Refactor.
 * This class exists to keep all the "explanation" and parsing of the packet
 * in one place, rather than spread out over layers of code.
 */
public class MedtronicResponse {
    private static final String TAG = "MedtronicResponse";
    protected byte[] raw;
    protected boolean mIsRadioResponse; // 1 byte, offset 0
    protected byte mUnknown1; // 1 byte, offset 1
    protected byte mRadioResponseStatus; // 1 byte, offset 2. 01 is ok, 04 is not. others?
    protected byte mUnknown2; // 1 byte, offset 3
    protected byte mUnknown3; // 1 byte, offset 4
    protected boolean mEOD; // 1 bit, MSB of offset 5
    protected int mResponseSize; // 15 bits, LS7B of offset 5 * 256 + offset 6
    protected byte mUnknown4; // 1 byte, offset 7 // I think U4&5 are showing this is
    protected byte mUnknown5; // 1 byte, offset 8 // a response to a query sent to pump
    protected byte[] mSerialNumber; // 3 bytes, offset 9
    protected byte mReceivedChecksum; // 1 byte, offset 12 (checksum of all preceding bytes.
    // the rest depends on what the pump sent us
    protected byte[] mPumpData; // unknown length, often 1 for simple responses (power_control, e.g.)
    protected byte mPumpDataChecksum; //1 byte, last byte of packet, checksum of mPumpData, or zero if no pump data.

    public MedtronicResponse() { init(); }

    public void init() {
        // initialize to unusable values, where possible.
        mIsRadioResponse = false;
        mRadioResponseStatus = (byte)0xFF;
        mEOD = true;
        mResponseSize = -1;
        mSerialNumber = new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF};
        mReceivedChecksum = (byte)0xFF;
        mPumpData = new byte[] {};
        mPumpDataChecksum = (byte)0xFF;
    }

    public boolean isRadioResponse() { return mIsRadioResponse; }
//    public boolean radioResponseOK() { return (mRadioResponseStatus == 0x01); }
    public boolean radioResponseOK() { return true; } // TODO: error check
    public boolean isEOD() { return mEOD; }
    public int getResponseSize() { return mResponseSize; } //how much the pump said it sent
    public byte[] getSerialNumber() { return mSerialNumber; }
    public byte getFrameChecksum() { return mReceivedChecksum; }
    public boolean frameChecksumOK() {
        return (mReceivedChecksum == CRC.crc8(raw, 12));
    }
    public byte[] getPumpData() { return mPumpData; }
    public byte getPumpDataChecksum() { return mPumpDataChecksum; }
    public boolean pumpDataChecksumOK() {
        if (mPumpData.length == 0) {
            return true;
        }
        return (mPumpDataChecksum == CRC.crc8(mPumpData));
    }

    public void parseFrom(byte[] rawRadioBuffer) {
        init(); // throw away any previous contents
        if (rawRadioBuffer == null) {
            return; // nothing to do.
        }
        if (rawRadioBuffer.length > 0) {
            // byte from carelink, saying this is a response to read radio
            mIsRadioResponse = (rawRadioBuffer[0] == 0x02);
        }
        // copy raw radio buffer, just for safe keeping
        raw = new byte[rawRadioBuffer.length];
        System.arraycopy(rawRadioBuffer,0,raw,0,rawRadioBuffer.length);
        // continue parsing
        if (rawRadioBuffer.length > 2) {
            mUnknown1 = rawRadioBuffer[1];
            // dunno what this is: receiving, overflow etc?
            mRadioResponseStatus = rawRadioBuffer[2];
        }
        if (rawRadioBuffer.length > 6) {
            mUnknown2 = rawRadioBuffer[3];
            mUnknown3 = rawRadioBuffer[4];
            mEOD = ((rawRadioBuffer[5] & 0x80) != 0);
            mResponseSize = ((0x7F & rawRadioBuffer[5]) << 8) | (0xFF & rawRadioBuffer[6]);
        }
        if (rawRadioBuffer.length > 11) {
            mUnknown4 = rawRadioBuffer[7]; // should be 0xA7?
            mUnknown5 = rawRadioBuffer[8]; // should be 0x01?
            mSerialNumber = new byte[3];
            System.arraycopy(rawRadioBuffer,9,mSerialNumber,0,3);
        }
        if (rawRadioBuffer.length > 12) {
            mReceivedChecksum = rawRadioBuffer[12];
        }
        // rest of packet is actual data from pump
        // We will trim the packet to the data the pump said it sent
        // Note, we may only get a partial response in this packet!
        // pump will try to send more than 64 bytes, but we are limited
        // to 64 bytes per USB/Carelink frame.
        // last byte is checksum of the actual data?
        if (rawRadioBuffer.length > 13) {
            int restOfPacketLength = rawRadioBuffer.length - 14;
            int pumpFullDataLength = mResponseSize;
            int pumpDataLength;
            if (pumpFullDataLength < restOfPacketLength) {
                pumpDataLength = pumpFullDataLength;
            } else {
                pumpDataLength = restOfPacketLength;
            }
            if (pumpDataLength > 0) {
                mPumpData = new byte[pumpDataLength];
                System.arraycopy(rawRadioBuffer,13,mPumpData,0,pumpDataLength);
            } else {
                mPumpData = new byte[] {};
            }
            mPumpDataChecksum = rawRadioBuffer[13 + pumpDataLength];
        }
        // now do sanity checks on what we've got.
        byte calculatedChecksum = CRC.crc8(mPumpData);
        if (calculatedChecksum!=mPumpDataChecksum) {
            //Log.e(TAG,String.format("Pump Data Checksum mismatch (0x%02X/0x%02X",calculatedChecksum,mPumpDataChecksum));
        }
    }
    public String explain() {
        String rval = "RadioResponse:";
        if (raw == null) {
            rval = rval + "(null)";
        } else if (raw.length == 0) {
            rval = rval + "(zero length)";
        } else rval = rval + String.format("(frame %d bytes)",raw.length);
        rval = rval + String.format("{%02X,SN%02X%02X%02X,Checksum %s,EOD=%s,%d bytes:%s}",
                mRadioResponseStatus,
                mSerialNumber[0],mSerialNumber[1],mSerialNumber[2],
                pumpDataChecksumOK() ? "OK" : "BAD",
                isEOD() ? "true" : "false",
                mPumpData == null ? 0 : mPumpData.length,
                ByteUtil.shortHexString(mPumpData));
        return rval;
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
