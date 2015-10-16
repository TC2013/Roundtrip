package com.gxwtech.rtdemo.medtronic;

import android.util.Log;

import com.gxwtech.droidbits.util.ByteUtil;
import com.gxwtech.rtdemo.HexDump;

/**
 * Created by Geoff on 4/27/15.
 */
public class MedtronicCommand {
    private static final String TAG = "MedtronicCommand";
    private static final boolean DEBUG_MEDTRONICCOMMAND = false;
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

    public MedtronicCommandStatusEnum run(byte[] serialNumber) {
        // Rewrite for bluetooth
        if (DEBUG_MEDTRONICCOMMAND) {
            Log.v("MEDTRONIC COMMAND", getName() + ": serial number " + ByteUtil.shortHexString(serialNumber));
        }

        // Send a packet to the Medtronic
        TransmitPacketCommand sender = new TransmitPacketCommand(
                mCode.opcode,
                mParams,
                serialNumber,
                mButton,
                mNRetries,
                calcRecordsRequired());
        CarelinkCommandStatusEnum senderStatus = CarelinkCommandStatusEnum.NONE;
        boolean resendDownloadRequest = true;
        int resendDownloadRetries = 0;
        int resendDownloadRetriesMax = 5;
        byte[] receivedData = new byte[0];
        while (resendDownloadRequest) {
            try {
                senderStatus = sender.run(carelink);
                // translate between a carelink status and a pump status
                if (senderStatus == CarelinkCommandStatusEnum.NONE) {
                    mStatus = MedtronicCommandStatusEnum.NONE;
                }
                if (senderStatus == CarelinkCommandStatusEnum.ACK) {
                    mStatus = MedtronicCommandStatusEnum.ACK;
                }
                if (senderStatus == CarelinkCommandStatusEnum.NACK) {
                    mStatus = MedtronicCommandStatusEnum.NACK;
                }
            } catch (UsbException e) {
                mStatus = MedtronicCommandStatusEnum.ERROR_USB;
            }

            if (DEBUG_MEDTRONICCOMMAND) {
                Log.v("MEDTRONIC COMMAND", "Sender status is " + senderStatus.toString());
            }
            // we've only just sent the packet, and expected the carelink to say "ok, I sent it."

            if (mSleepForPumpResponse > 0) {
                if (DEBUG_MEDTRONICCOMMAND) {
                    Log.v("MEDTRONIC COMMAND", String.format("Sleeping %d milliseconds before checking pump response.", mSleepForPumpResponse));
                }
                sleep(mSleepForPumpResponse);
            }
            try {
                receivedData = downloadIdeal(carelink, serialNumber);
                if (receivedData == null) {
                    Log.e(TAG, "downloadIdeal returned null buffer");
                } else {
                    if (DEBUG_MEDTRONICCOMMAND) {
                        Log.v(TAG, String.format("downloadIdeal reported %d bytes received", receivedData.length));
                    }
                    if (receivedData.length > 0) {
                        // got something, call it quits
                        resendDownloadRequest = false;
                        if (DEBUG_MEDTRONICCOMMAND) {
                            Log.v(TAG, String.format("Got %d bytes from radio buffer, ending run.", receivedData.length));
                        }
                    } else {
                        // got a zero length buffer from radio.  Try to read radio buffer again?
                        resendDownloadRetries++;
                        if (resendDownloadRetries > resendDownloadRetriesMax) {
                            // failed too many times.  Give up.
                            Log.e(TAG, String.format("Too many retries in reading radio buffer, giving up."));
                            resendDownloadRequest = false;
                        } else {
                            Log.w(TAG, String.format("Radio buffer gave us zero bytes.  Trying again %d/%d",
                                    resendDownloadRetries, resendDownloadRetriesMax));
                        }
                    }
                }
            } catch (UsbException e) {
                mStatus = MedtronicCommandStatusEnum.ERROR_USB;
                resendDownloadRequest = false;
                Log.e(TAG, "USB Exception: " + e.toString());
            }
        }
        mRawReceivedData = receivedData;
        // this is a hook to allow derived classes to get their data.
        if (mStatus == MedtronicCommandStatusEnum.ACK) {
            parse(receivedData);
        }

        if (DEBUG_MEDTRONICCOMMAND) {
            Log.v(TAG, "End of Medtronic downloadIdeal");
        }
        return mStatus;
    }

    protected int checkForData(Carelink carelink) throws UsbException {
        CarelinkCommandStatusEnum carelinkAck;
        int size = -1;
        CheckStatusCommand ck = new CheckStatusCommand();
        carelinkAck = ck.run(carelink);
        if (carelinkAck == CarelinkCommandStatusEnum.ACK) {
            size = ck.getReadSize();
        }
        if (DEBUG_MEDTRONICCOMMAND) {
            Log.v(TAG, "checkForData():CarelinkStatus is " + ck.responseToString());
        }
        return size;
    }

    public byte[] downloadIdeal(Carelink carelink, byte[] serialNumber) throws UsbException {
        int recordsExpected = calcRecordsRequired();
        int recordsReceived = 0;
        if (DEBUG_MEDTRONICCOMMAND) {
            Log.v(TAG, String.format("DownloadIdeal:recordsRequired=%d", recordsExpected));
        }
        boolean moreDataToGet = true;
        int tries = 0;
        byte[] mDataReceived = new byte[]{};
        byte[] rval = new byte[]{};
        while (moreDataToGet) {
            int bytesAvailable = 0;
            boolean keepTrying = true;
            while ((bytesAvailable == 0) && keepTrying) {
                bytesAvailable = checkForData(carelink);
                tries = tries + 1;
                if (bytesAvailable == -1) {
                    // error in checking for data
                    keepTrying = false;
                    moreDataToGet = false;
                    Log.e(TAG, "Error in checkForData");
                } else if ((bytesAvailable == 0) || (bytesAvailable == 14)) {
                    if (tries > mNRetries) {
                        moreDataToGet = false;
                        keepTrying = false;
                        Log.e(TAG, String.format("Download attempt %d/%d failed, giving up.", tries, mNRetries + 1));
                    } else {
                        Log.w(TAG, String.format("Download attempt %d/%d failed, sleeping %d millis to try again.", tries, mNRetries + 1, mSleepForPumpRetry));
                        if (bytesAvailable == 14) {
                            // This is the situation where the radio reports data available, but there's zero bytes.
                            /* This can happen if we take too long between asking the pump for data
                             * and trying to read the data back!
                             */
                            bytesAvailable = 0; // didn't really get anything from pump
                            // todo: fix hack?
                            mSleepForPumpResponse /= 2;
                            mSleepForPumpRetry /= 2;
                            Log.w(TAG, String.format("Carelink returned zero bytes. Trying shorter wait times: %dms/%dms",
                                    mSleepForPumpResponse, mSleepForPumpRetry));
                        } else {
                            sleep(mSleepForPumpRetry);
                        }

                    }
                } else {
                    // continue
                }
            }
            if (moreDataToGet && (bytesAvailable > 0)) {
                ReadRadioCommand rrcmd = new ReadRadioCommand(serialNumber, bytesAvailable);
                CarelinkCommandStatusEnum responseStatus = rrcmd.run(carelink);
                // check for command sent ok
                if (responseStatus != CarelinkCommandStatusEnum.ACK) {
                    Log.e(TAG, "ReadRadio command failed");
                    moreDataToGet = false;
                } else if (rrcmd.getResponse().getPumpData().length == 0) {
                    // sometimes we get an ACK from the stick, but the radio buffer
                    // has nothing (yet?).
                    Log.e(TAG, String.format("ReadRadio: zero bytes from radio"));
                    moreDataToGet = false;
                } else {
                    // Add new data to our collection:
                    // Must prepend data?
                    mDataReceived = ByteUtil.concat(mDataReceived, rrcmd.getResponse().getPumpData());
                    //mDataReceived = ByteUtil.concat(rrcmd.getResponse().getPumpData(),mDataReceived);
                    recordsReceived++;
                    if (DEBUG_MEDTRONICCOMMAND) {
                        Log.v(TAG, "Adding newly downloaded data. Now we have:\n"
                                + HexDump.dumpHexString(mDataReceived));
                    }
                    // If we've started to receive something, reset the tries
                    tries = 1;
                    boolean endOfData = rrcmd.getResponse().isEOD();
                    if (endOfData) {
                        if (DEBUG_MEDTRONICCOMMAND) {
                            Log.i(TAG, String.format("Found EOD, received %d bytes.", mDataReceived.length));
                            /* doesn't belong here, but for checking....*/
                            /*
                            if (mDataReceived.length >= 1022) {
                                byte[] first1022 = new byte[1022];
                                System.arraycopy(mDataReceived, 0, first1022, 0, 1022);
                                Log.v(TAG, String.format("Checksum of 1022 bytes: %s",
                                        HexDump.toHexString(CRC.calculate16CCITT(first1022))));
                            }
                            if (mDataReceived.length >= 1024) {
                                Log.v(TAG, String.format("Checksum of 1024 bytes: %s",
                                        HexDump.toHexString(CRC.calculate16CCITT(mDataReceived))));
                            }
                            */
                        }
                        rval = mDataReceived;
                        moreDataToGet = false;
                    }
                }
            }
        }
        if (recordsReceived < recordsExpected) {
            Log.w(TAG, String.format("Expected %d records, received %d.", recordsExpected, recordsReceived));
        }
        return rval;
    }

}
