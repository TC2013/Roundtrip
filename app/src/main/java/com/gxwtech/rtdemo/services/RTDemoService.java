package com.gxwtech.rtdemo.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
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
import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.MainActivity;
import com.gxwtech.rtdemo.MongoWrapper;
import com.gxwtech.rtdemo.PreferenceBackedStorage;
import com.gxwtech.rtdemo.R;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileEntry;
import com.gxwtech.rtdemo.medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.services.pumpmanager.PumpManager;
import com.gxwtech.rtdemo.services.pumpmanager.PumpSettingsParcel;
import com.gxwtech.rtdemo.services.pumpmanager.TempBasalPairParcel;
import com.gxwtech.rtdemo.usb.CareLinkUsb;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
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
    PendingIntent mRepeatingAlarmPendingIntent;
    public static final String REQUEST_STRING_EXTRANAME = "com.gxwtech.wltest2.requeststring";


    // GGW: I think APSLogic should be its own service, but for now, it's a member of RTDemoService.
    // It has significant connections with PumpManager which will have to be ironed out.
    APSLogic mAPSLogic;
    MongoWrapper mMongoWrapper;
    PreferenceBackedStorage mStorage;

    //protected static RTDemoService mInstance = null;
    NotificationManager mNM;

    private int NOTIFICATION = R.string.local_service_started;

    protected final int secondsBetweenRuns = 5 * 60; // five minutes
    protected int maxQueueLength = 100;
    protected ArrayList<String> msgQ = new ArrayList<>();

    // Intent for use in asking permission to use Carelink stick
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    PumpManager mPumpManager;

    public RTDemoService() {
        super(TAG);
        setIntentRedelivery(true);
    }

    public PumpManager getPumpManager() {
        return mPumpManager;
    }

    boolean deviceIsCarelink(UsbDevice device) {
        if (device == null) return false;
        // magic numbers for Carelink stick.
        return ((device.getVendorId() == CareLinkUsb.CARELINK_VENDOR_ID) && (device.getProductId() == CareLinkUsb.CARELINK_PRODUCT_ID));
    }

    // When the system broadcasts USB events, we'd like to know:
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                Log.w(TAG, "RTDemoService: ACTION_USB_PERMISSION");
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            Log.i("GGW", "Received Permission for device! (Carelink) (rebuild PumpManager?)");

                            mPumpManager = new PumpManager(getApplicationContext());
                            // needs application context to access USB manager
                            if (!mPumpManager.open()) {
                                Log.e(TAG, "Failed to open mPumpManager");
                                llog("Error opening Pump Manager");
                            }

                        }
                    } else {
                        Log.d(TAG, "Permission denied for device " + device.toString());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.w(TAG, "RTDemoService: ACTION_USB_DEVICE_DETACHED");
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    // call your method that cleans up and closes communication with the device
                    if (deviceIsCarelink(device)) {
                        llog("Carelink device lost");
                        // todo: need to detach cleanly
                        // this crashes MainActivity:
                        /*
                        mPumpManager.close();
                        mCarelinkDevice = null; // whack it, to force reloading
                        mUsbManager = null;  // whack it, to force reloading
                        */
                    } else {
                        //llog("USB device disconnected (not carelink):" + device.toString());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.w(TAG, "RTDemoService: ACTION_USB_DEVICE_ATTACHED");
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (deviceIsCarelink(device)) {
                    if (!getUsbManager().hasPermission(device)) {
                        llog("Carelink device attached, permission OK. (rebuild pumpManager?");
                    } else {
                        llog("Carelink device attached, permission NOT GRANTED. (rebuild pumpManager?)");
                    }
                    // TODO: need to re-attach cleanly.

                    mPumpManager = new PumpManager(getApplicationContext());
                    // needs application context to access USB manager
                    if (!mPumpManager.open()) {
                        Log.e(TAG, "Failed to open mPumpManager");
                        llog("Error opening Pump Manager");
                    }

                } else {
                    //llog("Other USB device attached:" + device.toString());
                }
            }
        }
    };

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
            if (srq.equals(Constants.SRQ.START_SERVICE)) {

                // Set up permissions for carelink
                getCarelinkPermission();
                // this BLOCKS until we get permission!
                // since it blocks, we can't do it in Create()

            } else if (srq.equals(Constants.SRQ.VERIFY_USB_PUMP_COMMUNICATIONS)) {
                getCarelinkPermission();
                checkPumpCommunications();
            } else if (srq.equals(Constants.SRQ.VERIFY_BLUETOOTH_PUMP_COMMUNICATIONS)) {

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();

                        BluetoothDevice rileyLink = null;
                        for (BluetoothDevice device : devices) {
                            if (device.getName().equals(Constants.PrefName.Bluetooth_RileyLink_Name)) {
                                rileyLink = device;
                            }
                        }

                        if (rileyLink != null) {
                            llog("RileyLink has been found.");

                            // Found the rileylink, what now...


                        } else {
                            llog("Could not find RileyLink.");
                        }

                    } else {
                        llog("Bluetooth is not enabled on device.");
                    }
                } else {
                    llog("Bluetooth is not available on device.");
                }
            } else if (srq.equals(Constants.SRQ.REPORT_PUMP_SETTINGS)) {
                PumpSettingsParcel parcel = new PumpSettingsParcel();
                parcel.initFromPumpSettings(mPumpManager.getPumpSettings());
                sendTaskResponseParcel(parcel, "PumpSettingsParcel");
            } else if (srq.equals(Constants.SRQ.REPORT_PUMP_HISTORY)) {

                // this was just used for debugging the getting of history reports.
                // TODO: Remove this and the pump history GUI, or fix them both to work properly
                Log.d(TAG, "Received request for pump history");
                HistoryReport report = mPumpManager.getPumpHistory(0);

            } else if (srq.equals(Constants.SRQ.SET_TEMP_BASAL)) {
                TempBasalPairParcel pair = (TempBasalPairParcel) intent.getParcelableExtra(Constants.ParcelName.TempBasalPairParcelName);
                Log.d(TAG, String.format("Request to Set Temp Basal(Rate %.2fU, duration %d minutes",
                        pair.mInsulinRate, pair.mDurationMinutes));
                mPumpManager.setTempBasal(pair);
            } else if (srq.equals(Constants.SRQ.MONGO_SETTINGS_CHANGED)) {
                // there are new settings in the preferences.
                // Get them and give them to MongoWrapper
                updateMongoWrapperFromPrefs();
            } else if (srq.equals(Constants.SRQ.START_REPEAT_ALARM)) {
                // MonitorActivity start button runs this.
                Log.w(TAG, "onHandleIntent: starting repeating alarm");
                //startRepeatingAlarm(3 * 1000); // first run begins in 3 seconds
                startRepeatingAlarm(secondsBetweenRuns * 1000); // first run begins 5 minutes from now
                // and run it once right now.
                serviceMain();
            } else if (srq.equals(Constants.SRQ.STOP_REPEAT_ALARM)) {
                // MonitorActivity stop button runs this.
                Log.w(TAG, "onHandleIntent: stopping repeating alarm");
                stopRepeatingAlarm();
            } else if (srq.equals(Constants.SRQ.DO_SUSPEND_MINUTES)) {
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

            } else if (srq.equals(Constants.SRQ.VERIFY_DB_ACCESS)) {
                // code removed.  todo: remove VERIFY_DB_ACCESS enum, too.
                // This should be sent from the timer, so that it happens in the right thread/right queue
            } else if (srq.equals(Constants.SRQ.APSLOGIC_STARTUP)) {
                serviceMain();
                // note: START_REPEAT_ALARM also calls serviceMain()
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

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCKNAME);
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }

    private void checkPumpCommunications() {
        // this command should be the first place
        // we actually try to talk over USB to the carelink.
        if (mPumpManager.wakeUpCarelink()) {
            llog("Carelink ready.");
            if (mPumpManager.verifyPumpCommunications()) {
                llog("Pump ready.");
            } else {
                llog("Error accessing pump.");
            }
        } else {
            llog("Error accessing CareLink USB Stick.");
            Log.e(TAG, "wakeUpCarelink failed");
        }
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

    // TODO: UGLY can we please find a way to do this asynchronously? i.e. no sleep!
    // For now, make all sleeps use this sleep, so that we can notify the UI.
    public void sleep(int millis) {
        if (millis > 1000) {
            // If we sleep for more than 1 second, notify the UI
            sendSleepNotification(DateTime.now(), millis / 1000);
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted: " + e.getMessage());
            e.printStackTrace();
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

        // create a PendingIntent to give to the USB Manager, to call us back with the result.
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // set up a filter for what broadcasts we wish to catch with mUsbReceiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // see EXTRA_DEVICE too
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        // set up the receiver with our filter
        registerReceiver(mUsbReceiver, filter);
        // still can't figure out where to do the creation/open properly:
        mPumpManager = new PumpManager(this);
        String serialNumber = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0).
                getString(Constants.PrefName.SerialNumberPrefName, MEDTRONIC_DEFAULT_SERIAL);
        // convert to bytes
        byte[] sn_bytes = HexDump.hexStringToByteArray(serialNumber);
        mPumpManager.setSerialNumber(sn_bytes);
        mPumpManager.open();
        mMongoWrapper = new MongoWrapper(getApplicationContext());
        updateMongoWrapperFromPrefs();

        mAPSLogic = new APSLogic(this, mPumpManager, mMongoWrapper);

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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new RTDemoBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */

    /**
     * In this case, our "client" is our UI thread/main activity
     */
    public class RTDemoBinder extends Binder {
        RTDemoService getService() {
            return RTDemoService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy()");
        /* This function runs in the Foreground */
        /* release resources */
        mPumpManager.close();
        mNM.cancel(NOTIFICATION);
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    // getting permission for the device is necessarily an asynchronous action
    private boolean getCarelinkPermission() {
        UsbDevice device = getCarelinkDevice();

        // If the device cannot be found, then the permissions are not given.
        if (device == null) {
            return false;
        }

        UsbManager manager = getUsbManager();
        if (manager.hasPermission(device)) {
            return true;
        }
        // create a PendingIntent to give to the USB Manager, to call us back with the result.
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // set up a filter for what broadcasts we wish to catch with mUsbReceiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // see EXTRA_DEVICE too
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        // set up the receiver with our filter
        registerReceiver(mUsbReceiver, filter);

        // ask for permission
        getUsbManager().requestPermission(device, mPermissionIntent);

        int loopcount = 0;
        // wait for permission (BLOCKING!)
        while (!getUsbManager().hasPermission(device)) {
            if (loopcount++ % 100 == 0) {
                Log.i("gapp", "Waiting for Carelink Permission");
            }
            // sleep for milliseconds
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                // whatever
                Log.i("gapp", "Exception(?):" + e.getMessage());
            }
        }
        // receiver no longer needed?
        //unregisterReceiver(mUsbReceiver);
        return true;
    }

    private UsbManager getUsbManager() {
        return (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    private UsbDevice mCarelinkDevice; // use getCarelinkDevice to get access

    private UsbDevice getCarelinkDevice() {
        // if we already have one, return it
        if (mCarelinkDevice != null) {
            Log.e(TAG, "Re-using existing carelink device");
            return mCarelinkDevice;
        }

        // else, try to go get it.
        HashMap<String, UsbDevice> deviceList = getUsbManager().getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        UsbDevice device = null;
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            if (deviceIsCarelink(device)) {
                break;
            } else {
                //Log.e(TAG, "Found a device, but it is not a CareLink:" + device.getDeviceName() + ", " + device.getProductName() + " (ProductID: " + device.getProductId() + ", VendorID: " + device.getVendorId() + ")");
                Log.e(TAG, "Found a device, but it is not a CareLink");
                device = null;
            }
        }
        mCarelinkDevice = device;
        if (mCarelinkDevice == null) {
            Log.e(TAG, "Failed to find suitable CareLink device");
        } else {
            Log.e(TAG, "Found new CareLink device");
        }

        return mCarelinkDevice; // NOTE: may still be null, if we couldn't find it!
    }

}
