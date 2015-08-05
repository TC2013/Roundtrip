package com.gxwtech.rtdemo.carelink;

import android.util.Log;

import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.carelink.util.ByteUtil;


/**
 * Created by geoff on 4/27/15.
 */
public class ProductInfoCommand extends CarelinkCommand {
    private static String TAG = "ProductInfoCommand";
    byte mRFFreq;
    byte[] mSerial;
    byte[] mProductVersion;
    byte[] mDescription;
    byte[] mSoftwareVersion;
    byte[] mInterfaces;

    public ProductInfoCommand() {
        init(CarelinkCommandEnum.CMD_C_PRODUCT_INFO);
    }

    public boolean respondedOK() {
        boolean ok = false;
        if (mRawResponse != null) {
            if (mRawResponse.length > 22) {
                ok = true;
            }
        }
        return ok;
    }

    public void parse() {
        if (respondedOK()) {
            mSerial = ByteUtil.substring(mRawResponse, 3, 3);
            mProductVersion = ByteUtil.substring(mRawResponse, 6, 2);
            mRFFreq = mRawResponse[8];
            mDescription = ByteUtil.substring(mRawResponse, 9, 10);
            mSoftwareVersion = ByteUtil.substring(mRawResponse, 19, 2);
            mInterfaces = ByteUtil.substring(mRawResponse, 21, mRawResponse.length - 22);
        }
    }

    public boolean isOK() {
        boolean rval = false; // assume false
        final byte[] good = {0x43, 0x6F, 0x6D, 0x4C, 0x69, 0x6E, 0x6B, 0x20, 0x49, 0x49};
        if (respondedOK()) {
            rval = true; // now assume true
            for (int i = 0; i < 10; i++) {
                if (mDescription[i] != good[i]) {
                    rval = false;
                }
            }
        }
        if (rval == false) {
            Log.e(TAG, "FAILED SANITY CHECK! product info does not match");
            if (respondedOK()) {
                Log.e(TAG, "Read this:" + HexDump.dumpHexString(mDescription));
                Log.e(TAG, "Should be:" + HexDump.dumpHexString(good));
            }

        }
        return rval;
    }
}
