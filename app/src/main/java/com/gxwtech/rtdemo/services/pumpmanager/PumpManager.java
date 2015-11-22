package com.gxwtech.rtdemo.services.pumpmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.RileyLink.ReadRadioCommand;
import com.gxwtech.RileyLink.RileyLink;
import com.gxwtech.RileyLink.RileyLinkCommandResult;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.medtronic.MedtronicCommand;
import com.gxwtech.rtdemo.medtronic.MedtronicCommandStatusEnum;
import com.gxwtech.rtdemo.medtronic.MedtronicResponse;
import com.gxwtech.rtdemo.medtronic.PowerControlCommand;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.medtronic.PumpData.PumpSettings;
import com.gxwtech.rtdemo.medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.medtronic.WakePumpCommand;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
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

    // constants for the pump-wakeup routines
    private static final int WAKEPUMP_MSG_PERIOD_MS = 80; // milliseconds
    private static final int timeout_millis=60; // this number must be less than WAKEPUMP_MSG_PERIOD_MS
    private static final int MAX_WAKEUP_SENDS = 128;
    private static final int WAKEUP_TRIES = 300 /*tries*/ * timeout_millis /*ms*/ / 1000 /*ms/sec*/; // ~18 seconds
    /* the wakeup-pump routine will take WAKEUP_TRIES * WAKEPUMP_MSG_PERIOD_MS milliseconds to time-out (~10s) */
    /* the send-wakeup-packet routine will send packets over a period of
     *    MAX_WAKEUP_SENDS * WAKEPUMP_MSG_PERIOD_MS seconds (~10.24 seconds)
     */

    // we need to keep track of the last time the PowerControl command was run,
    // so that if we're getting close to the end of the RF Transmitter 'on' time, we can rerun it.
    //protected Calendar mLastPowerControlRunTime;
    public static int VERIFY_MAX_RETRIES = 5;
    public static int VERIFY_MIN_SIGNAL = 100; // TODO totally arbitrary.

    RileyLink mRileyLink = null;
    byte[] mSerialNumber; // need a setter for this
    Context mContext;
    Handler taskHandler = new Handler();
    protected boolean pumpIsAwake = false;

    public PumpManager(Context context) {
        mContext = context;
        init();
    }

    /*
     *  To save power, the pump wakes up once every 5 seconds for a brief moment (how long?) to
     *  see if anyone's trying to talk to it.  To get its attention, we send a flood of attention
     *  messages:
     *
     *  From Gitter discussions:
     *  "It seems like the timing of the pump wake is as follows:
     *  Every 17ms, send the pump wake message (5D), for 8.488 seconds (so around 490 times…)"
     *  (Note, rate varies)
     *
     *  "After the last send you will receive a 0x06 message - around 10.8 seconds after your last send
     *  You will receive the 0x06 within a window of 10s or so. Sometimes quickly."
     *
     *  "For ble, I send about 128 over 10s.
     *  What happens is the pump is usually sleeping, but wakes up every 5 s or so to RX for a short
     *  period of time. You just need your packet to be there in that short window."
     *
     *  NOTE: that means that after our last packet has been sent, we still need to hang around for 12 seconds waiting.
     *  that means MAX_TRIES
     */
    public boolean wakePump() {
        pumpIsAwake = false; // assume it is asleep.
        // start the wake-pump messages flowing (asynchronously)
        wakeupSends = 0;
        taskHandler.post(wakePumpTask);
        int tries = 0;
        boolean done = false;
        while (!done) {
            ReadRadioCommand rrcmd = new ReadRadioCommand(mRileyLink, 64); // fixme: hard coded size?
            RileyLinkCommandResult result = rrcmd.run(mRileyLink, timeout_millis);
            if (result.mStatus == MedtronicResponse.STATUS_OK) {
                if (result.mPacket[0] == 0x06) {
                    pumpIsAwake = true;
                    done = true;
                } else {
                    Log.e(TAG,"Received packet during wakeup, but not what we expected: " + HexDump.dumpHexString(result.mPacket));
                }
            } else {
                tries++;
                if (tries > WAKEUP_TRIES) {
                    done = true;
                }
            }
        }
        return pumpIsAwake;
    }

    private static int wakeupSends= 0;
    private Runnable wakePumpTask = new Runnable() {
        @Override
        public void run() {
            if (!pumpIsAwake) {
                wakeupSends++;
                if (wakeupSends <= MAX_WAKEUP_SENDS) {
                    WakePumpCommand wakePumpCommand = new WakePumpCommand();
                    wakePumpCommand.run(mRileyLink, mSerialNumber);
                    // wait a bit, and send the next wakeup packet.
                    taskHandler.postDelayed(this, WAKEPUMP_MSG_PERIOD_MS);
                }
            }
        }
    };

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

    // Called when we have received permission to use Bluetooth device?
    public boolean open(RileyLink rileyLink) {
        mRileyLink = rileyLink;
        return true;
    }

    public void close() {
    }


    public void checkPowerControl() {
        if (!pumpIsAwake) {
            wakePump(); // this can block for up to 18.24 seconds (as of this writing)
        }

        byte minutesOfRFPower = (byte) 3; // TODO can set this to 3, or 10, or ?
        boolean runPowerControlCommand = false;

        DateTime lastPowerControlRunTime = getLastPowerControlRunTime();

        long timeDifference = Seconds.secondsBetween(lastPowerControlRunTime, DateTime.now()).getSeconds();

        long secondsRemaining = (minutesOfRFPower * 60 /* seconds per minute*/)
                - timeDifference;
        if (DEBUG_PUMPMANAGER) {
            Log.i(TAG, String.format("Seconds remaining on RF power: %d", secondsRemaining));
        }
        if (secondsRemaining < 60 /* seconds */) {
            runPowerControlCommand = true;
        }

        // now run it if we have to.
        if (runPowerControlCommand) {
            PowerControlCommand powerControlCommand = new PowerControlCommand((byte) 1, minutesOfRFPower);
            // the power control command can take a long time (>17 seconds) to run.
            // so get the new run time before running the command

            MedtronicCommandStatusEnum en = powerControlCommand.run(mRileyLink, mSerialNumber);
            if (DEBUG_PUMPMANAGER) {
                Log.i(TAG, "PowerControlCommand returned status: " + en.name());
            }
            // Only set the new run time if the command succeeded?
            setLastPowerControlRunTime(DateTime.now(DateTimeZone.UTC));
        }
    }



    public PumpSettings getPumpSettings() {
        checkPowerControl();
        PumpSettings settings = new PumpSettings();
        // TODO: get pumpSettings from pump
        return settings;
    }

    public HistoryReport getPumpHistory(int pageNumber) {
        HistoryReport report = new HistoryReport();
        return report;
    }

    public TempBasalPair getCurrentTempBasal() {
        TempBasalPair tbp = new TempBasalPair();
        // TODO: get tempBasalPair from pump
        return tbp;
    }

    public DateTime getRTCTimestamp() {
        return new DateTime();
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
        BasalProfile profile = new BasalProfile();
        // TODO: get profile from pump
        return profile;
    }

}