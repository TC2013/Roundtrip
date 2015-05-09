package com.gxwtech.rtdemo.Carelink;

import android.util.Log;

import com.gxwtech.rtdemo.Carelink.util.ByteUtil;
import com.gxwtech.rtdemo.Carelink.util.CRC8;
import com.gxwtech.rtdemo.Medtronic.MedtronicResponse;
import com.gxwtech.rtdemo.USB.UsbException;

/**
 * Created by geoff on 4/28/15.
 *
 * This command tells the carelink to put its radio buffer
 * on the USB connection.  The response may be 14 bytes from the
 * stick, plus 64 bytes of data BUT, we have to read them in
 * 64 byte chunks.
 *
 * So this command is different from others as it will
 * have to make two USB read requests.
 */

public class ReadRadioCommand extends CarelinkCommand {
    private static String TAG = "ReadRadioCommand";
    byte[] mSerialNumber;
    short mFullSize;
    short mCurrentSize;
    MedtronicResponse mResponse;

    /* size may be larger than 64 bytes, meaning we will have
     * to read the radio buffer twice.
     * This is because the Carelink uses 64 byte buffers, and
     * the pump uses 64 byte buffers.  So when we ask for packet
     * from the pump, the first 14 bytes are overhead from carelink,
     * and 14 bytes from the pump remain in the buffer.
     */
    public ReadRadioCommand(byte[] serialNumber, short size) {
        init(CarelinkCommandEnum.CMD_C_READ_RADIO);
        mSerialNumber = serialNumber;
        mFullSize = size;
        mCurrentSize = 0;
        mResponse = new MedtronicResponse();
    }

    //public byte[] getRadioRecord() { return mResponse.getPumpData(); }
    //public boolean isEOD() { return mResponse.isEOD();}
    public CarelinkCommandStatusEnum getCarelinkAck() {return mAck;}
    public MedtronicResponse getResponse() { return mResponse; }

    public int calcRecordsNeeded() {
        int rval = 1;
        if (mFullSize > 64) {
            rval = 2;
        }
        // should return number of 64 byte frames needed, but 1 or 2 suffices.
        return rval;
    }

    /*
     *  FindCRC will search for possible CRC matches in a buffer.
     *  It will look for all possible sub-strings, calculate the CRC for
     *  the substring and check the following byte to see if it matches
     *  the CRC.  It ignores CRCs of ZERO because zero occurs too often
     *  in data, so will spam our results. Useful for figuring out packet structure.
     */

    public static void findCRC(byte[] data) {
        if (data == null) {
            return;
        }
        if (data.length < 2) {
            return;
        }
        // i is start index
        // wl is word length;
        for (int wl = data.length-1; wl > 0; wl--) {
            for (int i = 0; i< data.length - wl; i++) {
                byte[] word = new byte[wl];
                System.arraycopy(data,i,word,0,wl);
                byte crc = CRC8.crc8(word);
                if (crc != 0) {
                    int j = i + wl;
                    if (crc == data[i + wl]) {
                        Log.i("CRC", String.format("CRC8(0x%02x) of data[%d-%d] found at offset %d", crc, i, i + wl - 1, j));
                    }
                }
            }
        }

    }

    // We override this command in ReadRadio because we have to download
    // more than one frame from the Carelink
    public CarelinkCommandStatusEnum run(Carelink stick) throws UsbException {
        byte[] response;
        byte[] packet;
        Log.w("ReadRadioCommand", String.format("Requested size is %d",mFullSize));
        // typically, mFullSize is:
        // 15, for a 1 byte response from pump
        // 14, for a 0 byte response from pump or
        // 78, for a 64 byte response from pump

        packet = new byte[] {0x0c, 0, ByteUtil.highByte(mFullSize), ByteUtil.lowByte(mFullSize)};
        packet = ByteUtil.concat(packet, CRC8.crc8(packet));

        mRawPacket = packet;
        Log.i("CARELINK COMMAND", getName());

        response = stick.doCommand(mRawPacket, 10, mFullSize);
        mRawResponse = response;
        parseAck(); // checks first bytes of mRawResponse for proper code

        if (mAck != CarelinkCommandStatusEnum.ACK) {
            Log.w(TAG,"Invalid ACK from Carelink, resetting stick, trying again.");
            stick.reset();
            response = stick.doCommand(mRawPacket,10, mFullSize);
            mRawResponse = response;
            parseAck(); // looks at mRawResponse, sets mAck
        }

        if (mAck != CarelinkCommandStatusEnum.ACK) {
            Log.w(TAG,"Invalid ACK -- failed twice. Quitting.");
        } else {

            if (calcRecordsNeeded() > 1) {
                /*
                 * this is actually a "link status" command,
                 * but it seems to make the stick cough up the rest of the packet.
                 */
                /*
                byte[] continueCommand = new byte[]{0x03, 0x00, 0x00};
                byte[] continuation = stick.doCommand(continueCommand, 100);
                response = ByteUtil.concat(response, continuation);
                */
            }
        }
        mRawResponse = response;
        // parseAck looks at mRawResponse
        parseAck(); // sets status to ACK or NACK, if 0x55 or 0x66

        mResponse.parseFrom(mRawResponse);
        Log.w(TAG,"Parsed response: " + mResponse.explain());
        return mAck;
    }

    protected void parseAck() {
        if (mRawResponse == null) {
            mAck = CarelinkCommandStatusEnum.NONE;
        } else if (mRawResponse.length == 0) {
            mAck = CarelinkCommandStatusEnum.NONE;
        } else if (mRawResponse[0] == 0x02) {
            mAck = CarelinkCommandStatusEnum.ACK;
        } else {
            mAck = CarelinkCommandStatusEnum.NACK;
        }
    }

    protected boolean dlAckd(byte[] response) {
        boolean rval = false;
        if (response!=null) {
            if (response.length > 0) {
                if (response[0] == 0x02) {
                    rval = true;
                }
            }
        }
        return rval;
    }
}
/*

Interesting response: We checked the link status, and it said 14 bytes available.
This is odd, because usually the response is either 15, or 78.
So, we request the 14 bytes in the radio buffer:
    0x00000000 0C 00 00 0E 5D                                  ....]
And we get this back from the pump:

    0x00000000 02 00 04 00 DD 80 00 A7 01 46 73 24 2A 00
Decoding this response:
    02 00 04: radio buffer error?
    00 DD: ?
    80 00: zero parameters, end of data?
    a7 01: ? ("this is a response to a transmitted packet"?)
    46 73 24: pump serial number
    2A: checksum of first 12 bytes
    00: ?

Here is normal:
Request 15 bytes:
    0x00000000 0C 00 00 0F C6
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