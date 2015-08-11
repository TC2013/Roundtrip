package com.gxwtech.rtdemo.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtdemo.BGReading;
import com.gxwtech.rtdemo.BGReadingParcel;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.MainActivity;
import com.gxwtech.rtdemo.MongoWrapper;
import com.gxwtech.rtdemo.PreferenceBackedStorage;
import com.gxwtech.rtdemo.R;
import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.Commands;
import com.gxwtech.rtdemo.bluetooth.GattAttributes;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicReadOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicWriteOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattInitializeBluetooth;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileEntry;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.services.pumpmanager.PumpManager;
import com.gxwtech.rtdemo.services.pumpmanager.PumpSettingsParcel;
import com.gxwtech.rtdemo.services.pumpmanager.TempBasalPairParcel;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by geoff on 4/9/15.
 * <p/>
 * This service handles all communication with the carelink stick
 * and the Medtronic pump.  MainActivity shouldn't access those directly.
 * <p/>
 * This class is mainly for handling the Android GUI/Background issues, like intents & messages.
 * Put the code for the pump into the PumpManager whenever possible.
 * <p/>
 * Unfortunately, it still has to handle USB issues, AFAICT, as these are OS issues.
 * <p/>
 * 2015-06-05 GGW: This class started out just being the background thread for pump communications,
 * but it has grown into being ALL background services.  There are really three very important
 * classes where the work is done: APSLogic, PumpManager and RTDemoService.  It is not a clean
 * heirarchy: they all call each other at various times.  The goal was to keep related code together
 * but it needs to be refactored.
 * <p/>
 * PumpManager: Handles pump data types and pump communications.
 * <p/>
 * APSLogic: does the insulin/TempBasal decision making.  It has to collect a lot of data to do this.
 * <p/>
 * RTDemoService: Handles anything Android related (except the android Log facility, which
 * I ended up using everywhere).
 */


public class RTDemoService extends IntentService {
    public static final String REQUEST_STRING_EXTRANAME = "com.gxwtech.wltest2.requeststring";
    private static final String TAG = "RTDemoService";
    // @TODO Move this to constants?
    private static final String MEDTRONIC_DEFAULT_SERIAL = "000000";
    private static final String MONGO_DEFAULT_SERVER = "localhost";
    private static final String MONGO_DEFAULT_PORT = "27015";
    private static final String MONGO_DEFAULT_DATABASE = "nightscout";
    private static final String MONGO_DEFAULT_USERNAME = "username";
    private static final String MONGO_DEFAULT_PASSWORD = "password";
    private static final String MONGO_DEFAULT_COLLECTION = "entries";
    private static final String WAKELOCKNAME = "com.gxwtech.RTDemo.RTDemoServiceWakeLock";

    private static volatile PowerManager.WakeLock lockStatic = null;
    protected final int secondsBetweenRuns = 5 * 60; // five minutes
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new RTDemoBinder();
    protected ArrayList<String> msgQ = new ArrayList<>();
    PendingIntent mRepeatingAlarmPendingIntent;
    // GGW: I think APSLogic should be its own service, but for now, it's a member of RTDemoService.
    // It has significant connections with PumpManager which will have to be ironed out.
    APSLogic mAPSLogic;
    MongoWrapper mMongoWrapper;
    PreferenceBackedStorage mStorage;
    //protected static RTDemoService mInstance = null;
    NotificationManager mNM;
    PendingIntent mPermissionIntent;

    PumpManager mPumpManager;
    private int NOTIFICATION = R.string.local_service_started;
    private UsbDevice mCarelinkDevice; // use getCarelinkDevice to get access

    public RTDemoService() {
        super(TAG);
        setIntentRedelivery(true);

    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCKNAME);
            lockStatic.setReferenceCounted(true);
        }

        return lockStatic;
    }

    public PumpManager getPumpManager() {
        return mPumpManager;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            // Everything here is run on a single background thread, one request at a time:
            String srq;
            srq = intent.getStringExtra("srq");
            if (srq == null) {
                srq = "(null)";
            }
            Log.w(TAG, String.format("onHandleIntent: received request srq=%s", srq));


            switch (srq) {
                case Constants.SRQ.BLUETOOTH_WRITE: {

                    BluetoothConnection conn = BluetoothConnection.getInstance(this);
                    byte[] command = Commands.getReadPumpCommand(new byte[]{0x41, 0x75, 0x40});

                    conn.queue(new GattCharacteristicWriteOperation(
                            UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                            UUID.fromString(GattAttributes.GLUCOSELINK_TX_PACKET_UUID),
                            command,
                            true,
                            true
                    ));

                    conn.queue(new GattCharacteristicWriteOperation(
                            UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                            UUID.fromString(GattAttributes.GLUCOSELINK_TX_TRIGGER_UUID),
                            new byte[]{0x01},
                            false,
                            false
                    ));

                    break;
                }
                case Constants.SRQ.BLUETOOTH_READ: {

                    BluetoothConnection conn = BluetoothConnection.getInstance(this);

                    conn.queue(new GattCharacteristicReadOperation(
                            UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                            UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT),
                            null
                    ));

                    break;
                }
                case Constants.SRQ.REPORT_PUMP_SETTINGS:
                    PumpSettingsParcel parcel = new PumpSettingsParcel();
                    parcel.initFromPumpSettings(mPumpManager.getPumpSettings());
                    sendTaskResponseParcel(parcel, "PumpSettingsParcel");
                    break;
                case Constants.SRQ.REPORT_PUMP_HISTORY:

                    // this was just used for debugging the getting of history reports.
                    // TODO: Remove this and the pump history GUI, or fix them both to work properly
                    Log.d(TAG, "Received request for pump history");
                    HistoryReport report = mPumpManager.getPumpHistory(0);

                    break;
                case Constants.SRQ.SET_TEMP_BASAL:
                    TempBasalPairParcel pair = (TempBasalPairParcel) intent.getParcelableExtra(Constants.ParcelName.TempBasalPairParcelName);
                    Log.d(TAG, String.format("Request to Set Temp Basal(Rate %.2fU, duration %d minutes",
                            pair.mInsulinRate, pair.mDurationMinutes));
                    mPumpManager.setTempBasal(pair);
                    break;
                case Constants.SRQ.MONGO_SETTINGS_CHANGED:
                    // there are new settings in the preferences.
                    // Get them and give them to MongoWrapper
                    updateMongoWrapperFromPrefs();
                    break;
                case Constants.SRQ.START_REPEAT_ALARM:
                    // MonitorActivity start button runs this.
                    Log.w(TAG, "onHandleIntent: starting repeating alarm");
                    //startRepeatingAlarm(3 * 1000); // first run begins in 3 seconds
                    startRepeatingAlarm(secondsBetweenRuns * 1000); // first run begins 5 minutes from now

                    // and run it once right now.
                    serviceMain();
                    break;
                case Constants.SRQ.STOP_REPEAT_ALARM:
                    // MonitorActivity stop button runs this.
                    Log.w(TAG, "onHandleIntent: stopping repeating alarm");
                    stopRepeatingAlarm();
                    break;
                case Constants.SRQ.DO_SUSPEND_MINUTES:
                    Log.w(TAG, "onHandleIntent: suspending repeating alarm");
                    // MonitorActivity Suspend button runs this.
                    // Get saved suspend duration from preferences.
                    // (Most likely, it was just saved by SuspendAPSActivity)
                    SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
                    int minutes = settings.getInt(Constants.PrefName.SuspendMinutesPrefName, 0);
                    if (minutes <= 0) {
                        Log.e(TAG, String.format("Cannot suspend for %d minutes.", minutes));
                    } else {
                        suspendRepeatingAlarm(minutes * 60 * 1000); // five minutes, should be.
                    }

                    break;
                case Constants.SRQ.VERIFY_DB_ACCESS:
                    // code removed.  todo: remove VERIFY_DB_ACCESS enum, too.
                    // This should be sent from the timer, so that it happens in the right thread/right queue
                    break;
                case Constants.SRQ.APSLOGIC_STARTUP:
                    serviceMain();
                    // note: START_REPEAT_ALARM also calls serviceMain()
                    break;
            }

        } finally {
            // The lock was grabbed in onStartCommand
            PowerManager.WakeLock lock = getLock(this.getApplicationContext());
            if (lock.isHeld()) {
                try {
                    lock.release();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(),
                            "Exception when releasing wakelock", e);
                }
            }
        }

    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    }

    public PendingIntent getAlarmPendingIntent() {
        if (mRepeatingAlarmPendingIntent == null) {
            int privateRequestCode = -99;
            Intent wakeupServiceIntent = new Intent(getApplicationContext(), RTDemoService.class).
                    putExtra("srq", Constants.SRQ.APSLOGIC_STARTUP);
            mRepeatingAlarmPendingIntent = PendingIntent.getService(getApplicationContext(), privateRequestCode,
                    wakeupServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return mRepeatingAlarmPendingIntent;
    }

    public void startRepeatingAlarm(int delayToFirstRunMillis) {
        int repeatingAlarmInterval = secondsBetweenRuns * 1000; // millis
        stopRepeatingAlarm();

        // TODO: Will always take the first branch of the if
        boolean use_elapsed_clock = true;
        if (use_elapsed_clock) {
            getAlarmManager().setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayToFirstRunMillis,
                    repeatingAlarmInterval, getAlarmPendingIntent());
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis() + delayToFirstRunMillis);

            getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    repeatingAlarmInterval, getAlarmPendingIntent());
        }
    }

    public void stopRepeatingAlarm() {
        getAlarmManager().cancel(getAlarmPendingIntent());
    }

    public void suspendRepeatingAlarm(int suspendMillis) {
        startRepeatingAlarm(suspendMillis);
    }

    private void testGetProfile() {
        BasalProfile b;
        int i;

        b = mPumpManager.getProfile(BasalProfileTypeEnum.STD);
        if (b.getEntries().isEmpty()) {
            Log.e(TAG, "testGetProfile: STD profile is empty");
        } else {
            Log.e(TAG, "testGetProfile: STD profile:");
            for (i = 0; i < b.getEntries().size(); i++) {
                BasalProfileEntry entry = b.getEntries().get(i);
                Log.d(TAG, String.format("rate: %.2f, start: %02d:%02d",
                        entry.rate, entry.startTime.getHourOfDay(), entry.startTime.getMinuteOfHour()));
            }
        }
        b = mPumpManager.getProfile(BasalProfileTypeEnum.A);
        if (b.getEntries().isEmpty()) {
            Log.e(TAG, "testGetProfile: A profile is empty");
        } else {
            Log.e(TAG, "testGetProfile: A profile:");
            for (i = 0; i < b.getEntries().size(); i++) {
                BasalProfileEntry entry = b.getEntries().get(i);
                Log.d(TAG, String.format("rate: %.2f, start: %02d:%02d",
                        entry.rate, entry.startTime.getHourOfDay(), entry.startTime.getMinuteOfHour()));
            }
        }
        b = mPumpManager.getProfile(BasalProfileTypeEnum.B);
        if (b.getEntries().isEmpty()) {
            Log.e(TAG, "testGetProfile: B profile is empty");
        } else {
            Log.e(TAG, "testGetProfile: B profile:");
            for (i = 0; i < b.getEntries().size(); i++) {
                BasalProfileEntry entry = b.getEntries().get(i);
                Log.d(TAG, String.format("rate: %.2f, start: %02d:%02d",
                        entry.rate, entry.startTime.getHourOfDay(), entry.startTime.getMinuteOfHour()));
            }
        }
    }

    // Let the UI know that we're sleeping (for pump communication delays)
    public void sendSleepNotification(DateTime starttime, int durationSeconds) {
        // send the log message to anyone who cares to listen (e.g. a UI component!)
        Intent intent = new Intent(Intents.ROUNDTRIP_SLEEP_MESSAGE)
                .putExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION, durationSeconds);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // send back to the UI thread an arbitrary response parcel
    protected void sendTaskResponseParcel(Parcelable p, String typename) {
        Intent intent = new Intent(Intents.ROUNDTRIP_TASK_RESPONSE);
        intent.putExtra("name", typename);
        intent.putExtra(typename, p);
        Log.d(TAG, "Sending task response parcel, name = " + typename);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // local log function that also posts to the front page status gui
    protected void llog(String msg) {
        // send the message to the Android logging service
        Log.i(TAG + "-LOG", msg);

        msgQ.add(msg);

        // send the log message to anyone who cares to listen (e.g. a UI component!)
        Intent intent = new Intent(Intents.ROUNDTRIP_STATUS_MESSAGE)
                .putExtra(Intents.ROUNDTRIP_STATUS_MESSAGE_STRING, msg)
                .putExtra("messages", msgQ);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // These need to be moved into PreferenceBackedStorage and loaded directly from MongoWrapper
    private void updateMongoWrapperFromPrefs() {
        // open prefs
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);

        // get strings from prefs
        String server = settings.getString(Constants.PrefName.MongoDBServerPrefName, MONGO_DEFAULT_SERVER);
        String serverPort = settings.getString(Constants.PrefName.MongoDBServerPortPrefName, MONGO_DEFAULT_PORT);
        String dbname = settings.getString(Constants.PrefName.MongoDBDatabasePrefName, MONGO_DEFAULT_DATABASE);
        String mongoUsername = settings.getString(Constants.PrefName.MongoDBUsernamePrefName, MONGO_DEFAULT_USERNAME);
        String mongoPassword = settings.getString(Constants.PrefName.MongoDBPasswordPrefName, MONGO_DEFAULT_PASSWORD);
        String mongoCollection = settings.getString(Constants.PrefName.MongoDBCollectionPrefName, MONGO_DEFAULT_COLLECTION);

        mMongoWrapper.updateURI(server, serverPort, dbname, mongoUsername, mongoPassword, mongoCollection);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* This function runs in the main thread (UI thread) */
        /* Here is where we do some initialization, but no work */
        Log.d(TAG, "onCreate()");

        mStorage = new PreferenceBackedStorage(getApplicationContext());

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        /* Make this service run in the foreground to make it harder to kill */
        /* Build a Notification Icon for the top left, to show we're running and provide access */
        /* This intent is used when the user chooses our notification icon */
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.START_RT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setContentTitle("RoundTrip_ct2")
                .setTicker("RoundTrip_ticker2")
                .setContentText("Data Collection2")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
        Notification notification = b.build();
        startForeground(Constants.NOTIFICATION_ID.RT_NOTIFICATION,
                notification);

        BluetoothConnection conn = BluetoothConnection.getInstance(this);

        // Init connection :)
        conn.queue(new GattInitializeBluetooth());

        //llog("End of onCreate()");
        llog("Roundtrip ready.");
    }

    // Here is where the wake-lock begins:
    // We've received a service startCommand, we grab the lock.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            PowerManager.WakeLock lock = getLock(this.getApplicationContext());

            if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
                lock.acquire();
            }

            // This will end up running onHandleIntent
            super.onStartCommand(intent, flags, startId);
        } else {
            Log.e(TAG, "Received null intent?");
        }
        return (START_REDELIVER_INTENT | START_STICKY);
    }

    public void checkAndUpdateBGReading() {
        // get latest BG reading from Mongo.  This can block, during internet access

        // check to see if our BGReading is old.
        BGReading latestBG = mStorage.getLatestBGReading();
        if ((latestBG.mTimestamp.isBefore(DateTime.now().minusMinutes(10)))
                || (latestBG.mTimestamp.isAfter(DateTime.now().plusMinutes(5)))) {
            mAPSLogic.broadcastAPSLogicStatusMessage("Accessing MongoDB for latest BG reading");
            MongoWrapper.BGReadingResponse bgResponse = mMongoWrapper.getBGReading();
            BGReading reading = bgResponse.reading;
            if (bgResponse.error) {
                mAPSLogic.broadcastAPSLogicStatusMessage("Error reading BG from mongo: " + bgResponse.errorMessage);
                if (reading != null) {
                    Log.e(TAG,
                            String.format("Error reading BG from Mongo: %s, and BG reading reports %.2f at %s",
                                    bgResponse.errorMessage,
                                    reading.mBg, reading.mTimestamp.toLocalDateTime().toString()));
                }
                // Are the contents of BGReading reading "ok to use", even with an error?
                // how else to handle?
            } else {
                // Save the reading
                mStorage.setLatestBGReading(reading);

                // broadcast the reading to the world. (esp. to MonitorActivity)
                Intent intent = new Intent(Intents.ROUNDTRIP_BG_READING);
                intent.putExtra("name", Constants.ParcelName.BGReadingParcelName);
                intent.putExtra(Constants.ParcelName.BGReadingParcelName, new BGReadingParcel(reading));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                Log.i(TAG, "Sending latest BG reading");
                mAPSLogic.broadcastAPSLogicStatusMessage(String.format("Latest BG reading reports %.2f at %s",
                        reading.mBg, reading.mTimestamp.toLocalDateTime().toString()));
            }
        }
    }

    protected void cleanupLogfiles() {
        int keepHours = mStorage.keepLogsForHours.get();
        // Check for logfiles older than 3 hours, delete them.
        //String[] fnames = fileList();
        ArrayList<String> fnames = new ArrayList<>();
        Pattern namePattern = Pattern.compile("RTLog_(.*)$");

        String path = Environment.getExternalStorageDirectory().toString();
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        Log.d("Files", "Size: " + file.length);
        for (int i = 0; i < file.length; i++) {
            Log.d("Files", "FileName:" + file[i].getName());
            fnames.add(file[i].getName());
        }

        for (int i = 0; i < fnames.size(); i++) {
            Matcher m = namePattern.matcher(fnames.get(i));
            if (m.find()) {
                String tstamp = m.group(1);
                Log.w(TAG, "Found logfile with time: " + tstamp);
                DateTime dt = DateTime.parse(tstamp);
                if (dt.isBefore(DateTime.now().minusHours(keepHours))) {
                    Log.w(TAG, "Deleting old file " + fnames.get(i));
                    //getApplicationContext().deleteFile(fnames.get(i));
                    File fd = new File(path, fnames.get(i));
                    boolean deleted = fd.delete();
                    Log.v(TAG, "successfully deleted = " + (deleted ? "True" : "False"));
                }
            } else {
                Log.w(TAG, "Not my logfile?: " + fnames.get(i));
            }
        }
    }

    protected void serviceMain() {
        checkAndUpdateBGReading();
        mAPSLogic.runAPSLogicOnce();
        cleanupLogfiles();
    }

    private void showNotification() {
        /* Build a Notification Icon for the top left, to show we're running and provide access */
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.START_RT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setContentTitle("RoundTrip_ct")
                .setTicker("RoundTrip_ticker")
                .setContentText("Data Collection")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
        Notification notification = b.build();
        mNM.notify(NOTIFICATION, notification);
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy()");

        super.onDestroy();
    }

    /**
     * In this case, our "client" is our UI thread/main activity
     */
    public class RTDemoBinder extends Binder {
        RTDemoService getService() {
            return RTDemoService.this;
        }
    }

}
