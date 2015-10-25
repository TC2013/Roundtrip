package com.gxwtech.RileyLink;

import android.os.SystemClock;
import android.util.Log;

import com.gxwtech.rtdemo.bluetooth.GattCharacteristicReadCallback;
import com.gxwtech.rtdemo.medtronic.MedtronicResponse;

/**
 * Created by geoff on 10/20/15.
 */
public class ReadRadioCommand implements RileyLinkCommand {
    private static final String TAG = "ReadRadioCommand";
    private static final boolean DEBUG_READRADIOCOMMAND = false;
    int mFullSize;
    int mCurrentSize;
    protected MedtronicResponse mResponse;

    /* size may be larger than 64 bytes, meaning we will have
     * to read the radio buffer twice.
     * This is because the Carelink uses 64 byte buffers, and
     * the pump uses 64 byte buffers.  So when we ask for packet
     * from the pump, the first 14 bytes are overhead from carelink,
     * and 14 bytes from the pump remain in the buffer.
     */
    public ReadRadioCommand(RileyLink rileylink, int size) {
        mFullSize = size;
        mCurrentSize = 0;
        mResponse = new MedtronicResponse();
    }

    public MedtronicResponse getResponse() { return mResponse; }

    public int calcRecordsNeeded() {
        int rval = 1;
        if (mFullSize > 64) {
            rval = 2;
        }
        // should return number of 64 byte frames needed, but 1 or 2 suffices.
        return rval;
    }

    // we may need to read multiple frames from the pump.
    public RileyLinkCommandResult run(RileyLink rileylink, int timeout_millis) {
        GattCharacteristicReadCallback cb = null;
        // prepare this response.
        mResponse.mStatus = MedtronicResponse.STATUS_NO_RESPONSE;
        mResponse.mPacket = null;
        mResponse.mStatusMessage = "Packet sent, no response from pump";
        // if we get a response from the pump, parseFrom() will overwrite this response
        rileylink.read(
                new GattCharacteristicReadCallback() {
                    @Override
                    public void call(byte[] characteristic) {
                        mResponse.parseFrom(characteristic);
                    }
                }
        );
        sleepForResponse(timeout_millis);
        return mResponse;
    }

    protected void sleepForResponse(int readTimeout_millis) {
        SystemClock.sleep(readTimeout_millis);
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
                byte crc = CRC.crc8(word);
                if (crc != 0) {
                    int j = i + wl;
                    if (crc == data[i + wl]) {
                        Log.v("CRC", String.format("CRC(0x%02x) of data[%d-%d] found at offset %d", crc, i, i + wl - 1, j));
                    }
                }
            }
        }

    }

}