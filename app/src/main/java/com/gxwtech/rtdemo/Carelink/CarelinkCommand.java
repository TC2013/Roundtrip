package com.gxwtech.rtdemo.Carelink;

import android.util.Log;

import com.gxwtech.rtdemo.USB.UsbException;

/**
 * Created by geoff on 4/27/15.
 *
 * This class will also be a place to record the hardware interaction, for later playback
 * Do not reuse commands that have already been run.  Make a new one.
 */

public class CarelinkCommand {
    private static final String TAG = "CarelinkCommand";
    protected CarelinkCommandEnum mCode;
    protected CarelinkCommandStatusEnum mAck;
    protected byte[] mRawPacket;
    protected byte[] mRawResponse;

    public CarelinkCommand() {
        init();
    }

    private void init() {
        mCode = CarelinkCommandEnum.CMD_C_INVALID;
        mAck = CarelinkCommandStatusEnum.NONE;
        mRawPacket = null;
        mRawResponse = null;
    }

    // easy access to initializer for derived classes
    protected void init(CarelinkCommandEnum which) {
        init();
        mCode = which;
    }

    public boolean isCommandComplete() {
        return (mAck != CarelinkCommandStatusEnum.NONE);
    }

    public byte[] getRawPacket() { return mRawPacket; }
    protected byte[] getRawResponse() { return mRawResponse; }
    public String getName() { return mCode.toString();}

    // this is a hook for subclasses to do what they need to do to the packet
    // This is called in run(), before sending the packet to the stick.
    protected byte[] preparePacket() {
        mRawPacket = new byte[3];
        mRawPacket[0] = mCode.opcode();
        mRawPacket[1] = 0;
        mRawPacket[2] = 0;
        return mRawPacket;
    }

    // Note: ReadRadio overrides this command, others shouldn't have to.
    public CarelinkCommandStatusEnum run(Carelink stick) throws UsbException {
        byte[] cmd;
        byte[] response;

        cmd = preparePacket();
        // TODO: check for null cmd return from preparePacket()
        mRawPacket = cmd;
        Log.i(TAG,getName());
        /* Ordinarily, we use a read-size of 64 bytes.  This is fine for
         * most commands, as they return 14 bytes or 15 bytes.
         * Exception is ReadRadioCommand which needs 78 bytes
         */
        response = stick.doCommand(mRawPacket,10, 64);
        mRawResponse = response;
        parseAck(); // checks first bytes of mRawResponse for proper code
        // (proper code is 0x01, 0x55 for normal commands
        //  and 0x02, 0xXX for ReadRadioCommand
        // We get out of sync with the carelink so easily,
        // I'm putting a retry in for every command.
        if (mAck != CarelinkCommandStatusEnum.ACK) {
            Log.w(TAG,"Invalid ACK from Carelink, resetting stick, trying again.");
            stick.reset();
            response = stick.doCommand(mRawPacket,10,64);
            mRawResponse = response;
            parseAck(); // looks at mRawResponse, sets mAck
        }
        parse();
        return mAck;
    }

    // ReadRadioCommand has to override this method. Others shouldn't.
    protected void parseAck() {
        if (mRawResponse!=null) {
            if (mRawResponse.length > 2) {
                if (mRawResponse[0] == 1) {
                    if (mRawResponse[1] == 0x55) {
                        mAck = CarelinkCommandStatusEnum.ACK;
                    } else if (mRawResponse[1] == 0x66) {
                        mAck = CarelinkCommandStatusEnum.NACK;
                    }
                }
             }
        }
    }

    // this is a hook for subclasses to interpret the response they got
    // this is called after run() has send the command and received an (immediate) answer
    protected void parse() {
        // mess with mRawResponse here.
        // For instance, resize mRawResponse to only contain valid data,
        // and not the trash left in the buffer from previous commands!
    }
}
