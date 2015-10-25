package com.gxwtech.rtdemo.medtronic;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.gxwtech.RileyLink.ReadRadioCommand;
import com.gxwtech.RileyLink.RileyLink;
import com.gxwtech.RileyLink.RileyLinkCommandResult;
import com.gxwtech.RileyLink.TransmitPacketCommand;
import com.gxwtech.droidbits.util.ByteUtil;
import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.GattCharacteristicReadCallback;

/**
 * Created by Geoff on 4/27/15.
 */
public class MedtronicCommand {
    private static final String TAG = "MedtronicCommand";
    private static final boolean DEBUG_MEDTRONICCOMMAND = false;
    private static final int default_timeout_millis = 100;
    private Context mContext;
    protected MedtronicCommandEnum mCode;
    protected MedtronicCommandStatusEnum mStatus;
    protected byte[] mPacket;
    protected byte[] mParams; // may be null!

    // button is zero, unless command 93 (SET_POWER_CONTROL) in which case, 85.
    // No, we don't know why :(
    protected byte mButton = 0;
    // +++ what's all this talk of a new way?
    protected byte[] mRawReceivedData;
    protected int mSleepForPumpResponse = 100;
    protected int mSleepForPumpRetry = 500; //millis
    byte mNRetries = 2;
    byte mBytesPerRecord = 64;
    byte mMaxRecords = 1;

    public MedtronicCommand() {
        init();
    }

    protected void init() {
        mCode = MedtronicCommandEnum.CMD_M_INVALID_CMD;
        mPacket = null;
        mParams = null;
        mButton = 0;
    }

    protected void init(MedtronicCommandEnum which) {
        init();
        mCode = which;
    }

    public MedtronicCommandEnum getCode() {
        return mCode;
    }

    public String getName() {
        return mCode.toString();
    }

    public byte calcRecordsRequired() {
        byte rval;
        int len = mBytesPerRecord * mMaxRecords;
        int i = len / 64;
        int j = len % 64;
        if (j > 0) {
            rval = (byte) (i + 1);
        } else {
            rval = (byte) i;
        }
        return rval;
    }

    // TODO: figure out how to get notification up to the gui that we're sleeping.
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // subclasses should override parse() and get data from mMResponse
    protected void parse(byte[] receivedData) {
        Log.w(TAG, "Base class parse called on command " + getName());
    }

    protected byte[] makePacket(MedtronicCommandEnum mCode, byte[] serialNumber, byte[] payload) {
        byte[] header = new byte[] {(byte)0xa7,serialNumber[0],serialNumber[1],serialNumber[2],mCode.opcode};
        byte[] rval = ByteUtil.concat(header,payload);
        return rval;
    }

    public MedtronicCommandStatusEnum run(RileyLink rileylink, byte[] serialNumber) {
        if (DEBUG_MEDTRONICCOMMAND) {
            Log.v("MEDTRONIC COMMAND", getName() + ": serial number " + ByteUtil.shortHexString(serialNumber));
        }

        //byte[] pkt_att = new byte[]{(byte) 0xa7, 0x46, 0x73, 0x24, (byte)0x8d, 0x00};

        // Send a packet to the Medtronic
        // TODO: I'm not sure at all if mButton is correct here.  Could just be 0x00?
        byte[] packetToSend = makePacket(mCode,serialNumber,new byte[] {mButton});

        TransmitPacketCommand sender = new TransmitPacketCommand(rileylink,packetToSend
                /*
                mCode.opcode,
                mParams,
                serialNumber,
                mButton,
                mNRetries,
                calcRecordsRequired()
                */
        );

        // sending function does not block.
        RileyLinkCommandResult senderStatus = sender.run(rileylink, default_timeout_millis);
        // wait for response from pump
        sleepForPumpResponse(mSleepForPumpResponse);

        return mStatus;
    }

    void sleepForPumpResponse(int milliseconds) {
        SystemClock.sleep(milliseconds);
    }

}
