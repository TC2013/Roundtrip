package com.gxwtech.rtdemo.Medtronic;

import android.util.Log;

import com.gxwtech.rtdemo.HexDump;

/**
 * Created by Geoff on 5/2/2015.
 */
public class ReadHistoryCommand extends MedtronicCommand {
    private static final String TAG = "ReadHistoryCommand";
    public ReadHistoryCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_HISTORY);
        mNRetries = 2;
        mMaxRecords = 2;
        mSleepForPumpResponse = 8000;
        mSleepForPumpRetry = 1000;
        mParams = new byte[] { 0x00 }; // 0x02? 0x03?
    }

    public void parse(byte[] receivedData) {
        int d = receivedData.length;
        Log.e(TAG,"parse: " + HexDump.dumpHexString(receivedData));
    }

}

/*
From original run-history example:
First command (tell pump to send history)
01 00 a7 01 46 73 24 80 01 00 02 02 00 80 f6 00 00

01 00 a7 01 (Send command to pump)
46 73 24 (pump serial)
80 01 (one parameter)
00 (button)
02 (two retries)
02 (two records expected/requested)
00 (unknown zero)
80 (read history command code)
f6 (checksum for packet so far)
00 (page number?)
00 (checksum for parameters)

pump responded with 64 bytes.

We sent:
01 00 A7 01 46 73 24 80 01 00 02 02 00 80 F6 00 00

same, but we got zero-byte response from pump.  Why?
 */
