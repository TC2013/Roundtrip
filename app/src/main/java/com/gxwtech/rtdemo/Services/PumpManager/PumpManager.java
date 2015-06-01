package com.gxwtech.rtdemo.Services.PumpManager;

import android.content.Context;
import android.util.Log;

import com.gxwtech.rtdemo.Carelink.Carelink;
import com.gxwtech.rtdemo.Carelink.ProductInfoCommand;
import com.gxwtech.rtdemo.Carelink.SignalStrengthCommand;
import com.gxwtech.rtdemo.Medtronic.MedtronicCommandStatusEnum;
import com.gxwtech.rtdemo.Medtronic.PowerControlCommand;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.Medtronic.PumpData.PumpSettings;
import com.gxwtech.rtdemo.Medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.Medtronic.ReadHistoryCommand;
import com.gxwtech.rtdemo.Medtronic.ReadProfileCommand;
import com.gxwtech.rtdemo.Medtronic.ReadPumpSettingsCommand;
import com.gxwtech.rtdemo.Medtronic.SetTempBasalCommand;
import com.gxwtech.rtdemo.USB.CareLinkUsb;
import com.gxwtech.rtdemo.USB.UsbException;

import java.util.Calendar;

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

    // we need to keep track of the last time the PowerControl command was run,
    // so that if we're getting close to the end of the RF Transmitter 'on' time, we can rerun it.
    protected Calendar mLastPowerControlRunTime;

    // need access to context to get at UsbManager
    public PumpManager(Context context) {
        mContext = context;
        init();
    }

    public boolean setSerialNumber(byte[] serialNumber) {
        if (serialNumber == null) { return false; }
        if (serialNumber.length != 3) { return false; }
        System.arraycopy(serialNumber,0,mSerialNumber,0,3);
        Log.w(TAG, String.format("New serial number: %02X%02X%02X", mSerialNumber[0], mSerialNumber[1], mSerialNumber[2]));
        return true;
    }

    protected void init() {
        mSerialNumber = new byte[] {0,0,0};
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

    public void checkPowerControl() {
        byte minutesOfRFPower = (byte)10; // can set this to 3, or 10, or ?
        boolean runPowerControlCommand = false;
        if (mLastPowerControlRunTime == null) {
            // haven't run it yet.  Do so.
            runPowerControlCommand = true;
        } else {
            long timeDifference = Calendar.getInstance().getTimeInMillis()
                    - mLastPowerControlRunTime.getTimeInMillis();
            long secondsRemaining = (minutesOfRFPower * 60 /* seconds per minute*/)
                    - (timeDifference / 1000 /* millis per second*/);
            Log.w(TAG,String.format("Seconds remaining on RF power: %d",secondsRemaining));
            if (secondsRemaining < 60 /* seconds */) {
                runPowerControlCommand = true;
            }
        }
        // now run it if we have to.
        if (runPowerControlCommand) {
            PowerControlCommand powerControlCommand = new PowerControlCommand((byte)1,minutesOfRFPower);
            // the power control command can take a long time (>17 seconds) to run.
            // so get the new run time before running the command
            Calendar newRunTime = Calendar.getInstance();
            MedtronicCommandStatusEnum en = powerControlCommand.run(mCarelink,mSerialNumber);
            Log.w(TAG,"PowerControlCommand returned status: " + en.name());
            // Only set the new run time if the command succeeded?
            mLastPowerControlRunTime = newRunTime;
        }
    }

    public PumpSettings getPumpSettings() {
        checkPowerControl();
        ReadPumpSettingsCommand cmd = new ReadPumpSettingsCommand();
        cmd.run(mCarelink,mSerialNumber);
        return cmd.getPumpSettings();
    }

    public void getPumpHistory() {
        checkPowerControl();
        ReadHistoryCommand rhcmd = new ReadHistoryCommand();
        //rhcmd.testParser();
        rhcmd.run(mCarelink,mSerialNumber);
    }

    // insulinRate is in Units, granularity 0.025U
    // durationMinutes is in minutes, granularity 30min
    // both values will be checked and floor'd.
    public void setTempBasal(double insulinRate, int durationMinutes) {
        checkPowerControl();
        SetTempBasalCommand cmd = new SetTempBasalCommand(insulinRate,durationMinutes);
        cmd.run(mCarelink,mSerialNumber);
        // todo: check for success?
    }

    public void setTempBasal(TempBasalPair pair) {
        setTempBasal(pair.mInsulinRate, pair.mDurationMinutes);
    }

    public BasalProfile getProfile(BasalProfileTypeEnum which) {
        checkPowerControl();
        ReadProfileCommand rpcmd = new ReadProfileCommand();
        rpcmd.setProfileType(which);
        rpcmd.run(mCarelink, mSerialNumber);
        BasalProfile profile = rpcmd.getProfile();
        return profile;
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
