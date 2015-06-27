package com.gxwtech.rtdemo.services.pumpmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtdemo.carelink.Carelink;
import com.gxwtech.rtdemo.carelink.ProductInfoCommand;
import com.gxwtech.rtdemo.carelink.SignalStrengthCommand;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.medtronic.MedtronicCommandStatusEnum;
import com.gxwtech.rtdemo.medtronic.PowerControlCommand;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.medtronic.PumpData.PumpSettings;
import com.gxwtech.rtdemo.medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.medtronic.PumpData.records.BolusWizard;
import com.gxwtech.rtdemo.medtronic.ReadBasalTempCommand;
import com.gxwtech.rtdemo.medtronic.ReadHistoryCommand;
import com.gxwtech.rtdemo.medtronic.ReadProfileCommand;
import com.gxwtech.rtdemo.medtronic.ReadPumpRTCCommand;
import com.gxwtech.rtdemo.medtronic.ReadPumpSettingsCommand;
import com.gxwtech.rtdemo.medtronic.SetTempBasalCommand;
import com.gxwtech.rtdemo.medtronic.TempBasalEvent;
import com.gxwtech.rtdemo.usb.CareLinkUsb;
import com.gxwtech.rtdemo.usb.UsbException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;

/**
 * Created by geoff on 5/8/15.
 *
 * Another layer of separation!
 * This serves to separate the background thread from the
 * pump management operations.
 *
 * The pump-manager is re-instantiated when the Carelink is plugged/unplugged.
 * Be careful if you're trying to cache data in the pump manager, or
 * if you're caching a reference to the pump manager anywhere (esp. APSLogic)
 */
public class PumpManager {
    private static final String TAG = "PumpManager";
    CareLinkUsb stick; // The USB connection
    Carelink mCarelink; // The CarelinkCommand runner, built from a CareLinkUsb
    byte[] mSerialNumber; // need a setter for this
    Context mContext;

    // we need to keep track of the last time the PowerControl command was run,
    // so that if we're getting close to the end of the RF Transmitter 'on' time, we can rerun it.
    //protected Calendar mLastPowerControlRunTime;

    public PumpManager(Context context) {
        mContext = context;
        init();
    }

    protected DateTime getLastPowerControlRunTime() {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        // get strings from prefs
        String lastPower = settings.getString(Constants.PrefName.LastPowerControlRunTime, "never");
        Log.d(TAG,"getLastPowerControlRunTime reports preference value: " + lastPower);
        if (lastPower==null) {
            lastPower = "never";
        }
        if ("never".equals(lastPower)) {
            Log.d(TAG,"getLastPowerControlRunTime: returning invalid DateTime");
            return new DateTime(0);
        }
        DateTime rval = ISODateTimeFormat.dateTime().parseDateTime(lastPower);
        Log.d(TAG,"getLastPowerControlRunTime: returning (ISO) " + rval.toString());
        return rval;
    }

    protected void setLastPowerControlRunTime(DateTime when_iso) {
        Log.d(TAG, "setLastPowerControlRunTime: setting new run time: " + ISODateTimeFormat.dateTime().print(when_iso));
        mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName,0).
                edit().putString(Constants.PrefName.LastPowerControlRunTime, ISODateTimeFormat.dateTime().print(when_iso))
                .commit();

    }

    public boolean setSerialNumber(byte[] serialNumber) {
        if (serialNumber == null) {
            return false;
        }
        if (serialNumber.length != 3) {
            return false;
        }
        System.arraycopy(serialNumber, 0, mSerialNumber, 0, 3);
        Log.w(TAG, String.format("New serial number: %02X%02X%02X", mSerialNumber[0], mSerialNumber[1], mSerialNumber[2]));
        return true;
    }

    protected void init() {
        mSerialNumber = new byte[]{0, 0, 0};
    }

    // Called when we have received permission to use USB device
    public boolean open() {
        boolean openedOK = true;
        try {
            stick = new CareLinkUsb();
            stick.open(mContext);
            mCarelink = new Carelink(mContext,stick);
        } catch (UsbException e) {
            Log.e(TAG,"Error on USB open: " + e.toString());
            openedOK = false;
        }
        return openedOK;
    }

    // Called AFTER USB device has been removed.
    public void close() {
        try {
            stick.close();
        } catch (UsbException e) {
            Log.e(TAG,"Error on USB close: " + e.toString());
        }
    }

    public static int WAKE_UP_TIMEOUT_MS = 200;
    public static int WAKE_UP_MAX_RETRIES = 5;
    public boolean wakeUpCarelink() {
        int wakeupRetries = 0;
        boolean awake = false;

        // Phase 1: see if the stick will respond with product info
        while ((!awake) && (++wakeupRetries <= WAKE_UP_MAX_RETRIES)) {
            Log.i(TAG, "ProductInfo");
            ProductInfoCommand picmd = new ProductInfoCommand();
            try {
                picmd.run(mCarelink);
            } catch (UsbException e) {
                Log.e(TAG, "USB exception(?):" + e.getMessage());
            }
            if (!picmd.isOK()) {
                if (wakeupRetries == WAKE_UP_MAX_RETRIES) {
                    Log.e(TAG, "Stick failed sanity check. Replug stick.");
                } else {
                    Log.w(TAG, String.format("Stick failed sanity check, sleep %d millis and try again %d/%d", WAKE_UP_TIMEOUT_MS, wakeupRetries, WAKE_UP_MAX_RETRIES));
                    sleep(WAKE_UP_TIMEOUT_MS);
                }
            } else {
                awake = true;
                Log.w(TAG, "Stick is awake.");
            }
        }
        return awake;
    }

    public static int VERIFY_TIMEOUT_MS = 500;
    public static int VERIFY_MAX_RETRIES = 5;
    public static int VERIFY_MIN_SIGNAL = 100; // TODO totally arbitrary.
    public boolean verifyPumpCommunications() {
        boolean canHearPump = false;
        // phase 2: see if it can see the pump with decent signal strength
        int findPumpRetries = 0;
        while ((!canHearPump) && (findPumpRetries < VERIFY_MAX_RETRIES)) {
            SignalStrengthCommand sscmd = new SignalStrengthCommand();
            try {
                sscmd.run(mCarelink);
            } catch (UsbException e) {
                Log.e(TAG, "UsbException when running SignalStrengthCommand:" + e.getMessage());
            }
            int signalStrength = sscmd.getSignalStrength();
            Log.i(TAG, String.format("SignalStrength reports %d", signalStrength));
            if (signalStrength < VERIFY_MIN_SIGNAL) {
                findPumpRetries++;
                if (findPumpRetries < VERIFY_MAX_RETRIES) {
                    Log.w(TAG, String.format("SignalStrength too low, try again (%d/%d)", findPumpRetries, VERIFY_MAX_RETRIES));
                    sleep(VERIFY_TIMEOUT_MS);
                } else {
                    Log.e(TAG, String.format("SignalStrength too low, retries exceeded."));
                }
            } else {
                canHearPump = true;
                Log.w(TAG, "Stick can hear pump.");
            }
        }
        return canHearPump;
    }

    public void checkPowerControl() {
        byte minutesOfRFPower = (byte) 10; // TODO can set this to 3, or 10, or ?
        boolean runPowerControlCommand = false;

        DateTime lastPowerControlRunTime = getLastPowerControlRunTime();

        long timeDifference = Seconds.secondsBetween(lastPowerControlRunTime,DateTime.now()).getSeconds();

        long secondsRemaining = (minutesOfRFPower * 60 /* seconds per minute*/)
                - timeDifference;
        Log.w(TAG, String.format("Seconds remaining on RF power: %d", secondsRemaining));
        if (secondsRemaining < 60 /* seconds */) {
            runPowerControlCommand = true;
        }

        // now run it if we have to.
        if (runPowerControlCommand) {
            PowerControlCommand powerControlCommand = new PowerControlCommand((byte) 1, minutesOfRFPower);
            // the power control command can take a long time (>17 seconds) to run.
            // so get the new run time before running the command

            MedtronicCommandStatusEnum en = powerControlCommand.run(mCarelink, mSerialNumber);
            Log.w(TAG, "PowerControlCommand returned status: " + en.name());
            // Only set the new run time if the command succeeded?
            setLastPowerControlRunTime(DateTime.now(DateTimeZone.UTC));
        }
    }

    public PumpSettings getPumpSettings() {
        checkPowerControl();
        ReadPumpSettingsCommand cmd = new ReadPumpSettingsCommand();
        cmd.run(mCarelink, mSerialNumber);
        return cmd.getPumpSettings();
    }

    public HistoryReport getPumpHistory(int pageNumber) {
        // TODO: pass success or failure code back to caller
        checkPowerControl();
        ReadHistoryCommand rhcmd = new ReadHistoryCommand();
        //rhcmd.testParser();
        rhcmd.setPageNumber(pageNumber);
        MedtronicCommandStatusEnum cmdStatus = rhcmd.run(mCarelink, mSerialNumber);
        if ((cmdStatus == MedtronicCommandStatusEnum.ACK) && (rhcmd.mParsedOK)) {
            Log.d(TAG,"ReadHistoryCommand reports success on page " + pageNumber);
        } else {
            Log.d(TAG, "ReadHistoryCommand reports failure on page " + pageNumber);
        }
        return rhcmd.mHistoryReport;
    }

    public TempBasalPair getCurrentTempBasal() {
        checkPowerControl();
        ReadBasalTempCommand cmd = new ReadBasalTempCommand();
        cmd.run(mCarelink, mSerialNumber);
        // TODO: check for success
        return cmd.getTempBasalPair();
    }

    public DateTime getRTCTimestamp() {
        checkPowerControl();
        ReadPumpRTCCommand cmd = new ReadPumpRTCCommand();
        cmd.run(mCarelink, mSerialNumber);
        // TODO: check for success
        return cmd.getRTCTimestamp();
    }

    // insulinRate is in Units, granularity 0.025U
    // durationMinutes is in minutes, granularity 30min
    // both values will be checked and floor'd.
    // returns true on success, false on any error
    public boolean setTempBasal(double insulinRate, int durationMinutes) {
        boolean success = false;
        checkPowerControl();
        SetTempBasalCommand cmd = new SetTempBasalCommand(insulinRate, durationMinutes);
        MedtronicCommandStatusEnum cmdSuccess = cmd.run(mCarelink, mSerialNumber);
        if (cmdSuccess == MedtronicCommandStatusEnum.ACK) {
            success = true;
        }
        return success;
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

    public void sleep(int millis) {
        if (millis > 1000) {
            // If we sleep for more than 1 second, notify the UI
            // Let the UI know that we're sleeping (for pump communication delays)
            // send the log message to anyone who cares to listen (e.g. a UI component!)
            Intent intent = new Intent(Intents.ROUNDTRIP_SLEEP_MESSAGE)
                    .putExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION,millis/1000);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted: " + e.getMessage());
            e.printStackTrace();
        }
    }
}