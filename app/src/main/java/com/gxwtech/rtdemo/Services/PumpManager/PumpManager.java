package com.gxwtech.rtdemo.Services.PumpManager;

import android.content.Context;
import android.util.Log;

import com.gxwtech.rtdemo.Carelink.Carelink;
import com.gxwtech.rtdemo.Carelink.ProductInfoCommand;
import com.gxwtech.rtdemo.Carelink.SignalStrengthCommand;
import com.gxwtech.rtdemo.Medtronic.PumpData.PumpSettings;
import com.gxwtech.rtdemo.Medtronic.ReadPumpSettingsCommand;
import com.gxwtech.rtdemo.USB.CareLinkUsb;
import com.gxwtech.rtdemo.USB.UsbException;

/**
 * Created by geoff on 5/8/15.
 *
 * Another layer of separation!
 * This serves to separate the background thread from the
 * pump management operations.
 */
public class PumpManager {
    private static final String TAG = "PumpManager";
    CareLinkUsb stick; // The USB connection
    Carelink mCarelink; // The CarelinkCommand runner, built from a CareLinkUsb
    Context mContext;
    byte[] mSerialNumber; // need a setter for this

    // need access to context to get at UsbManager
    public PumpManager(Context context) {
        mContext = context;
        init();
    }

    public boolean setSerialNumber(byte[] serialNumber) {
        if (serialNumber == null) { return false; }
        if (serialNumber.length != 3) { return false; }
        System.arraycopy(mSerialNumber,0,serialNumber,0,3);
        return true;
    }

    protected void init() {
        mSerialNumber = new byte[3];
        // todo: fix this default
        // can simply delete it and require RTDemoService to set it.
        byte[] sn = {0x46,0x73,0x24};
        setSerialNumber(sn);
    }

    // Called when we have received permission to use USB device
    public boolean open() {
        boolean openedOK = true;
        try {
            stick = new CareLinkUsb(mContext);
            stick.open();
            mCarelink = new Carelink(stick);
        } catch (UsbException e) {
            openedOK = false;
        }
        return openedOK;
    }

    // Called AFTER USB device has been removed.
    public void close() {
    }

    public boolean wakeUpCarelink() {
        int maxWakeupRetries = 5;
        int wakeupRetries = 0;
        boolean awake = false;
        // Phase 1: see if the stick will respond with product info
        while ((!awake) && (wakeupRetries < maxWakeupRetries)) {
            Log.i(TAG,"ProductInfo");
            ProductInfoCommand picmd = new ProductInfoCommand();
            try {
                picmd.run(mCarelink);
            } catch (UsbException e) {
                Log.e(TAG,"USB exception(?):" + e.getMessage());
            }
            if (!picmd.isOK()) {
                wakeupRetries++;
                if (wakeupRetries == maxWakeupRetries) {
                    Log.e(TAG, "Stick failed sanity check. Replug stick.");
                } else {
                    int sleep_millis = 200;
                    Log.w(TAG, String.format(
                            "Stick failed sanity check, sleep %d millis and try again %d/%d",sleep_millis, wakeupRetries, maxWakeupRetries));
                    sleep(sleep_millis);
                }
            } else {
                awake = true;
                Log.w(TAG, "Stick is awake.");
            }
        }
        return awake;
    }
    public boolean verifyPumpCommunications() {
        boolean canHearPump = false;
        // phase 2: see if it can see the pump with decent signal strength
        int findPumpRetries=0;
        int maxFindPumpRetries = 5;
        int minimumSignalStrength = 100; // totally arbitrary.
        while ((!canHearPump) && (findPumpRetries < maxFindPumpRetries)) {
            SignalStrengthCommand sscmd = new SignalStrengthCommand();
            try {
                sscmd.run(mCarelink);
            } catch (UsbException e) {
                Log.e(TAG,"UsbException when running SignalStrengthCommand:" + e.getMessage());
            }
            int signalStrength = sscmd.getSignalStrength();
            Log.i(TAG,String.format("SignalStrength reports %d", signalStrength));
            if (signalStrength < minimumSignalStrength) {
                findPumpRetries++;
                if (findPumpRetries < maxFindPumpRetries) {
                    Log.w(TAG, String.format("SignalStrength too low, try again (%d/%d)", findPumpRetries,maxFindPumpRetries));
                    sleep(500);
                } else {
                    Log.e(TAG, String.format("SignalStrength too low, retries exceeded."));
                }
            } else {
                canHearPump = true;
                Log.w(TAG,"Stick can hear pump.");
            }
        }
        return canHearPump;
    }

    public PumpSettings getPumpSettings() {
        ReadPumpSettingsCommand cmd = new ReadPumpSettingsCommand();
        cmd.run(mCarelink,mSerialNumber);
        return cmd.getPumpSettings();
    }

    // TODO: UGLY can we please find a way to do this asynchronously? i.e. no sleep!
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG,"Sleep interrupted: " + e.getMessage());
        }
    }


}
