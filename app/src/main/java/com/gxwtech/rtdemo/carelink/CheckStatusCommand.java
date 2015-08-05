package com.gxwtech.rtdemo.carelink;

import android.util.Log;

import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.carelink.util.StringUtil;

import java.util.ArrayList;

/**
 * Created by geoff on 4/27/15.
 */
public class CheckStatusCommand extends CarelinkCommand {
    byte mStatusByte;
    int mReadSize;

    public CheckStatusCommand() {
        init(CarelinkCommandEnum.CMD_C_LINK_STATUS);
        mStatusByte = -1;
        mReadSize = -1;
    }

    public byte getStatusByte() {
        return mStatusByte;
    }

    public int getReadSize() {
        return mReadSize;
    }

    protected void parse() {
        byte[] response = getRawResponse();
        if (response != null) {
            if (response.length > 7) {
                if (mAck == CarelinkCommandStatusEnum.ACK) {
                    mStatusByte = response[5];
                    mReadSize = ((0x7F & response[6]) << 8) | (0xFF & response[7]);
                } else {
                    // NACK'd command?
                }
            } else {
                // response too short!
            }
        } else {
            // response is null! has command been run yet?
        }

        // got back: (01 55) 00 00 02 00 00 00 05 04 00
//        0000   0x01 0x55 0x00 0x00 0x02 0x01 0x00 0x0f    .U......
//        0008   0x05 0x04 0x00 0x00 0x00 0x00 0x1b 0x00    ........
    }

    // responseToString() can be called after the command is run.
    public String responseToString() {
        ArrayList<String> ra = new ArrayList<>();
        ra.add(mAck.toString());
        ra.add(HexDump.toHexString(mStatusByte));
        ra.add(String.format("readSize:%d", mReadSize));
        if ((mStatusByte & 0x01) > 0) {
            // Status: OK
            ra.add("Data Available");
        }
        if ((mStatusByte & 0x02) > 0) {
            // Status: Receive in progress
            ra.add("Receiving");
        }
        if ((mStatusByte & 0x04) > 0) {
            // Status: transmit in progress
            ra.add("Transmitting");
        }
        if ((mStatusByte & 0x08) > 0) {
            // Status: interface error
            ra.add("Interface Error");
        }
        if ((mStatusByte & 0x10) > 0) {
            // Status: Receive overflow
            ra.add("Receive Overflow");
        }
        if ((mStatusByte & 0x20) > 0) {
            // Status: Transmit overflow
            ra.add("Transmit Overflow");
        }
        if ((mStatusByte & 0x80) > 0) {
            Log.e("CheckStatusCommand", "Unknown link status 0x80");
            ra.add("UNKNOWN");
        }
        String rval = "LinkStatus:{" + StringUtil.join(ra, ",") + "}";
        return rval;
    }
}

