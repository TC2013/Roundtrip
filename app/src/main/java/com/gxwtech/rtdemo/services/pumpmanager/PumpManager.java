package com.gxwtech.rtdemo.services.pumpmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.medtronic.PumpData.PumpSettings;
import com.gxwtech.rtdemo.medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.usb.CareLinkUsb;
import com.gxwtech.rtdemo.usb.UsbException;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by geoff on 5/8/15.
 * <p/>
 * Another layer of separation!
 * This serves to separate the background thread from the
 * pump management operations.
 * <p/>
 * The pump-manager is re-instantiated when the Carelink is plugged/unplugged.
 * Be careful if you're trying to cache data in the pump manager, or
 * if you're caching a reference to the pump manager anywhere (esp. APSLogic)
 */
public class PumpManager {
    private static final String TAG = "PumpManager";
    private static final boolean DEBUG_PUMPMANAGER = false;
    public static int WAKE_UP_TIMEOUT_MS = 200;
    public static int WAKE_UP_MAX_RETRIES = 5;
    public static int VERIFY_TIMEOUT_MS = 500;

    // we need to keep track of the last time the PowerControl command was run,
    // so that if we're getting close to the end of the RF Transmitter 'on' time, we can rerun it.
    //protected Calendar mLastPowerControlRunTime;
    public static int VERIFY_MAX_RETRIES = 5;
    public static int VERIFY_MIN_SIGNAL = 100; // TODO totally arbitrary.
    CareLinkUsb stick; // The USB connection
    byte[] mSerialNumber; // need a setter for this
    Context mContext;

    public PumpManager(Context context) {
        mContext = context;
        init();
    }

    protected DateTime getLastPowerControlRunTime() {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        // get strings from prefs
        String lastPower = settings.getString(Constants.PrefName.LastPowerControlRunTime, "never");
        if (DEBUG_PUMPMANAGER) {
            Log.d(TAG, "getLastPowerControlRunTime reports preference value: " + lastPower);
        }
        if (lastPower == null) {
            lastPower = "never";
        }
        if ("never".equals(lastPower)) {
            if (DEBUG_PUMPMANAGER) {
                Log.d(TAG, "getLastPowerControlRunTime: returning invalid DateTime");
            }
            return new DateTime(0);
        }
        DateTime rval = ISODateTimeFormat.dateTime().parseDateTime(lastPower);
        if (DEBUG_PUMPMANAGER) {
            Log.d(TAG, "getLastPowerControlRunTime: returning (ISO) " + rval.toString());
        }
        return rval;
    }

    protected void setLastPowerControlRunTime(DateTime when_iso) {
        if (DEBUG_PUMPMANAGER) {
            Log.d(TAG, "setLastPowerControlRunTime: setting new run time: " + ISODateTimeFormat.dateTime().print(when_iso));
        }
        mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0).
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
        if (DEBUG_PUMPMANAGER) {
            Log.w(TAG, String.format("New serial number: %02X%02X%02X", mSerialNumber[0], mSerialNumber[1], mSerialNumber[2]));
        }
        return true;
    }

    protected void init() {
        mSerialNumber = new byte[]{0, 0, 0};
    }

    // Called AFTER USB device has been removed.
    public void close() {
        try {
            stick.close();
        } catch (UsbException e) {
            Log.e(TAG, "Error on USB close: " + e.toString());
        }
    }

    public boolean wakeUpCarelink() {
        int wakeupRetries = 0;
        boolean awake = false;

        // Phase 1: see if the stick will respond with product info
        while ((!awake) && (++wakeupRetries < WAKE_UP_MAX_RETRIES)) {

        }
        return awake;
    }

    public boolean verifyPumpCommunications() {

        return false;
    }

    public void checkPowerControl() {


    }

    public PumpSettings getPumpSettings() {

        return null;
    }

    public HistoryReport getPumpHistory(int pageNumber) {
        return null;
    }

    public TempBasalPair getCurrentTempBasal() {
        return null;
    }

    public DateTime getRTCTimestamp() {
        return null;
    }

    // insulinRate is in Units, granularity 0.025U
    // durationMinutes is in minutes, granularity 30min
    // both values will be checked and floor'd.
    public void setTempBasal(double insulinRate, int durationMinutes) {
        return;
    }

    public void setTempBasal(TempBasalPair pair) {
        setTempBasal(pair.mInsulinRate, pair.mDurationMinutes);
    }

    public BasalProfile getProfile(BasalProfileTypeEnum which) {
        return null;
    }

    public void sleep(int millis) {
        if (millis > 1000) {
            // If we sleep for more than 1 second, notify the UI
            // Let the UI know that we're sleeping (for pump communication delays)
            // send the log message to anyone who cares to listen (e.g. a UI component!)
            Intent intent = new Intent(Intents.ROUNDTRIP_SLEEP_MESSAGE)
                    .putExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION, millis / 1000);
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