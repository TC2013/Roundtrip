package com.gxwtech.rtdemo.Carelink;

import android.content.Context;
import android.util.Log;

import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.USB.CareLinkUsb;
import com.gxwtech.rtdemo.USB.UsbException;

/**
 * Created by geoff on 4/27/15.
 */

/*
 * This is intended to be the "front half" of the carelink connection, with CareLinkUsb
 * being the "back half".  This is intended to provide a clean(ish) interface for future
 * implementations.
 */
public class Carelink {
    private CareLinkUsb mStick;
    public Context mContext;

    public Carelink(Context context, CareLinkUsb stick) {
        mContext = context;
        mStick = stick;
    }

    public void reset() throws UsbException {
        if (mStick != null) {
            mStick.close();
            mStick.open(mContext);
        }
    }

    protected byte[] doCommand(byte[] command,int delayMillis, int readSize) throws UsbException {
        // can save raw transmit/receive here
        Log.i("doCommand", "WRITING COMMAND TO CARELINK\n" + HexDump.dumpHexString(command));
        byte[] result = mStick.sendCommand(command,delayMillis, readSize);
        Log.i("doCommand","READING CARELINK RESPONSE TO COMMAND:\n" + HexDump.dumpHexString(result));
        return result;
    }

}
