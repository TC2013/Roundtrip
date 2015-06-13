package com.gxwtech.rtdemo.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.gxwtech.rtdemo.BGReading;
import com.gxwtech.rtdemo.BGReadingParcel;
import com.gxwtech.rtdemo.Carelink.util.ByteUtil;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.MainActivity;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfileEntry;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.Medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.Medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.MongoWrapper;
import com.gxwtech.rtdemo.R;
import com.gxwtech.rtdemo.Services.PumpManager.PumpManager;
import com.gxwtech.rtdemo.Services.PumpManager.PumpSettingsParcel;
import com.gxwtech.rtdemo.Services.PumpManager.TempBasalPairParcel;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by geoff on 4/9/15.
 *
 * This service handles all communication with the carelink stick
 * and the medtronic pump.  MainActivity shouldn't access those directly.
 *
 * This class is mainly for handling the Android GUI/Background issues, like intents & messages.
 * Put the code for the pump into the PumpManager whenever possible.
 *
 * Unfortunately, it still has to handle USB issues, AFAICT, as these are OS issues.
 *
 * 2015-06-05 GGW: This class started out just being the background thread for pump communications,
 * but it has grown into being ALL background services.  There are really three very important
 * classes where the work is done: APSLogic, PumpManager and RTDemoService.  It is not a clean
 * heirarchy: they all call each other at various times.  The goal was to keep related code together
 * but it needs to be refactored.
 *
 * PumpManager: Handles pump data types and pump communcations.
 *
 * APSLogic: does the insulin/TempBasal decision making.  It has to collect a lot of data to do this.
 *
 * RTDemoService: Handles anything Android related (except the android Log facility, which
 * I ended up using everywhere).
 *
 */


public class RTDemoService extends Service {
    private static final String TAG = "RTDemoService";

    // GGW: I think APSLogic should be its own service, but for now, it's a member of RTDemoService.
    // It has significant connections with PumpManager which will have to be ironed out.
    APSLogic mAPSLogic;
    MongoWrapper mMongoWrapper;

    protected static RTDemoService mInstance = null;
    NotificationManager mNM;
    Looper mServiceLooper;
    ServiceHandler mServiceHandler = null;

    // for giving us a signal to run the APS Logic every 5 minutes
    // Does this need to be separate from mServiceHandler?
    // feels clumsy.  The mTimerRunnable will post an intent to start
    // the next run of APSLogic, and will reset itself to run again.
    Handler mTimerHandler;
    TimerRunnable mTimerRunnable = new TimerRunnable();

    private int NOTIFICATION = R.string.local_service_started;

    protected final int secondsBetweenRuns = 5 * 60; // five minutes
    protected int maxQueueLength = 100;
    protected BlockingDeque<String> messageQ = new LinkedBlockingDeque<>(maxQueueLength);

    // Intent for use in asking permission to use Carelink stick
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    PumpManager mPumpManager;

    public static RTDemoService getInstance() {
        return mInstance;
    }
    public PumpManager getPumpManager() { return mPumpManager; }

    boolean deviceIsCarelink(UsbDevice device) {
        if (device == null) return false;
        // magic numbers for Carelink stick.
        return ((device.getVendorId() == 2593) && (device.getProductId() == 32769));
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            Log.i("GGW", "Received Permission for device! (Carelink)");
                            mPumpManager = new PumpManager(getApplicationContext());
                            // needs application context to access USB manager
                            if (!mPumpManager.open()) {
                                Log.e(TAG,"Failed to open mPumpManager");
                                llog("Error opening Pump Manager");
                            }
                        }
                    }
                    else {
                        Log.d(TAG, "Permission denied for device " + device.toString());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
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
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (deviceIsCarelink(device)) {
                    if (!getUsbManager().hasPermission(device)) {
                        llog("Carelink device attached, permission OK.");
                    } else {
                        llog("Carelink device attached, permission NOT GRANTED.");
                    }
                    // TODO: need to re-attach cleanly.
                    mPumpManager = new PumpManager(getApplicationContext());
                    // needs application context to access USB manager
                    if (!mPumpManager.open()) {
                        Log.e(TAG,"Failed to open mPumpManager");
                        llog("Error opening Pump Manager");
                    }
                } else {
                    //llog("Other USB device attached:" + device.toString());
                }
            }
        }
    };

    /* Private class ServiceHandler that receives messages from the thread */
    // TODO: This interface between GUI and should be rewritten!
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            /*
            This is run in the background (service) thread.
              It is handling messages passed to it by the foreground thread.
             */
            int startId = msg.arg1;
            //llog("handleMessage: received msg, arg1="+startId);
            Object someObject = msg.obj; // dunno what it is yet

            if (msg.arg2 == Constants.SRQ.START_SERVICE) {

                // Set up permissions for carelink
                getCarelinkPermission();
                // this BLOCKS until we get permission!
                // since it blocks, we can't do it in Create()

            } else if (msg.arg2 == Constants.SRQ.VERIFY_PUMP_COMMUNICATIONS) {
                checkPumpCommunications();
            } else if (msg.arg2 == Constants.SRQ.SET_SERIAL_NUMBER) {
                // ewww....
                byte[] serialNumber = (byte[])msg.obj;
                Log.w(TAG,"Service received set serial number, serialNumber=" + ByteUtil.shortHexString(serialNumber));
                mPumpManager.setSerialNumber(serialNumber);
            } else if (msg.arg2 == Constants.SRQ.REPORT_PUMP_SETTINGS) {
                PumpSettingsParcel parcel = new PumpSettingsParcel();
                parcel.initFromPumpSettings(mPumpManager.getPumpSettings());
                sendTaskResponseParcel(parcel, "PumpSettingsParcel");
            } else if (msg.arg2 == Constants.SRQ.REPORT_PUMP_HISTORY) {
                Log.d(TAG, "Received request for pump history");
                HistoryReport report = mPumpManager.getPumpHistory();

            } else if (msg.arg2 == Constants.SRQ.SET_TEMP_BASAL) {
                TempBasalPairParcel pair = (TempBasalPairParcel) (msg.obj);
                Log.d(TAG, String.format("Request to Set Temp Basal(Rate %.2fU, duration %d minutes",
                        pair.mInsulinRate, pair.mDurationMinutes));
                mPumpManager.setTempBasal(pair);
            } else if (msg.arg2 == Constants.SRQ.MONGO_SETTINGS_CHANGED) {
                // there are new settings in the preferences.
                // Get them and give them to MongoWrapper
                updateMongoWrapperFromPrefs();
            } else if (msg.arg2 == Constants.SRQ.PERSONAL_PREFERENCE_CHANGE) {
                updateFromPersonalPrefs();
            } else if (msg.arg2 == Constants.SRQ.START_AUTO_MODE) {
                // MonitorActivity start button runs this.
                serviceRepeat();
            } else if (msg.arg2 == Constants.SRQ.STOP_AUTO_MODE) {
                // MonitorActivity stop button runs this.
                stopServiceRepeatTimer();
            } else if (msg.arg2 == Constants.SRQ.DO_SUSPEND_MINUTES) {
                // MonitorActivity Suspend button runs this.
                // Get saved suspend duration from preferences.
                // (Most likely, it was just saved by SuspendAPSActivity)
                SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
                int minutes = settings.getInt(Constants.PrefName.SuspendMinutesPrefName, 0);
                if (minutes <= 0) {
                    Log.e(TAG,String.format("Cannot suspend for %d minutes.",minutes));
                } else {
                    suspendServiceRepeatTimer(minutes);
                }

            } else if (msg.arg2 == Constants.SRQ.VERIFY_DB_ACCESS) {
                // code removed.  todo: remove VERIFY_DB_ACCESS enum, too.
                // This should be sent from the timer, so that it happens in the right thread/right queue
            } else if (msg.arg2 == Constants.SRQ.APSLOGIC_STARTUP) {
                serviceMain();
            } else {
                // just wait half second
                long endTime = System.currentTimeMillis() + 500;
                while (System.currentTimeMillis() < endTime) {
                    synchronized (this) {
                        try {
                            wait(endTime - System.currentTimeMillis());
                            //llog("(Service has updated from data sources)");
                        } catch (Exception e) {
                            llog(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void checkPumpCommunications() {
        // this command should be the first place
        // we actually try to talk over USB to the carelink.
        if (!mPumpManager.wakeUpCarelink()) {
            llog("Error accessing Carelink USB Stick.");
            //Log.e(TAG,"wakeUpCarelink failed");
        } else {
            llog("Carelink ready.");
        }
        if (!mPumpManager.verifyPumpCommunications()) {
            llog("Error accessing pump.");
        } else {
            llog("Pump ready.");
        }
    }

    private void testGetProfile() {
        BasalProfile b;
        int i;

        b = mPumpManager.getProfile(BasalProfileTypeEnum.STD);
        if (b.getEntries().isEmpty()) {
            Log.e(TAG,"testGetProfile: STD profile is empty");
        } else {
            Log.e(TAG,"testGetProfile: STD profile:");
            for (i=0; i<b.getEntries().size(); i++) {
                BasalProfileEntry entry = b.getEntries().get(i);
                Log.d(TAG,String.format("rate: %.2f, start: %02d:%02d",
                        entry.rate, entry.startTime.getHourOfDay(),entry.startTime.getMinuteOfHour()));
            }
        }
        b = mPumpManager.getProfile(BasalProfileTypeEnum.A);
        if (b.getEntries().isEmpty()) {
            Log.e(TAG, "testGetProfile: A profile is empty");
        } else {
            Log.e(TAG, "testGetProfile: A profile:");
            for (i=0; i<b.getEntries().size(); i++) {
                BasalProfileEntry entry = b.getEntries().get(i);
                Log.d(TAG,String.format("rate: %.2f, start: %02d:%02d",
                        entry.rate, entry.startTime.getHourOfDay(),entry.startTime.getMinuteOfHour()));
            }
        }
        b = mPumpManager.getProfile(BasalProfileTypeEnum.B);
        if (b.getEntries().isEmpty()) {
            Log.e(TAG, "testGetProfile: B profile is empty");
        } else {
            Log.e(TAG, "testGetProfile: B profile:");
            for (i=0; i<b.getEntries().size(); i++) {
                BasalProfileEntry entry = b.getEntries().get(i);
                Log.d(TAG,String.format("rate: %.2f, start: %02d:%02d",
                        entry.rate, entry.startTime.getHourOfDay(),entry.startTime.getMinuteOfHour()));
            }
        }

    }

    // TODO: UGLY can we please find a way to do this asynchronously? i.e. no sleep!
    // For now, make all sleeps use this sleep, so that we can notify the UI.
    public static void sleep(int millis) {
        if (millis > 1000) {
            // If we sleep for more than 1 second, notify the UI
            RTDemoService.getInstance().sendSleepNotification(DateTime.now(), millis / 1000);
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG,"Sleep interrupted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Let the UI know that we're sleeping (for pump communication delays)
    public void sendSleepNotification(DateTime starttime, int durationSeconds) {
        // send the log message to anyone who cares to listen (e.g. a UI component!)
        Intent intent = new Intent(Intents.ROUNDTRIP_SLEEP_MESSAGE)
                .putExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION,durationSeconds);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // receive an xDrip BGReading and send it to the APSLogic module.
    // (this exists to make RTDemoService the public interface to the rest of the world,
    // keeping APSLogic in internal module)
    public void receiveXDripBGEstimate(BGReading bgr) {
        mAPSLogic.receiveXDripBGReading(bgr);
    }

    // This function is used by APSLogic to send a message to the MonitorActivity log window
    // This function exists to keep Android stuff out of APSLogic
    public void broadcastAPSLogicStatusMessage(String message) {
        Intent intent = new Intent(Intents.APSLOGIC_LOG_MESSAGE);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // send back to the UI thread an arbitrary response parcel
    protected void sendTaskResponseParcel(Parcelable p, String typename) {
        Intent intent = new Intent(Intents.ROUNDTRIP_TASK_RESPONSE);
        intent.putExtra("name",typename);
        intent.putExtra(typename, p);
        Log.d(TAG, "Sending task response parcel, name = " + typename);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // local log function that also posts to the front page status gui
    protected void llog(String msg){

        // send the message to the Android logging service
        Log.i(TAG + "-LOG",msg);

        // record the message in our own list of recent log messages
        if (messageQ.size() > maxQueueLength) {
            messageQ.removeFirst();
        }
        messageQ.add(msg);

        // send the log message to anyone who cares to listen (e.g. a UI component!)
        Intent intent = new Intent(Intents.ROUNDTRIP_STATUS_MESSAGE)
                .putExtra(Intents.ROUNDTRIP_STATUS_MESSAGE_STRING, msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // function to retrieve a list of recent messages
    // do we need to make this threadsafe?
    public List<String> getRecentMessages(int howmany) {
        List<String> rval = new ArrayList<String>();
        if ((howmany < 1)||(howmany > messageQ.size())) {
            howmany = messageQ.size();
        }
        Iterator<String> i = messageQ.descendingIterator();
        while(i.hasNext() && (howmany > 0)) {
            howmany--;
            // 'new' here is probably not necessary (?)
            rval.add(0,new String(i.next()));
        }
        return rval;
    }

    // this is done here, because I'm trying to keep the MongoWrapper from getting too much android stuff in it.
    private void updateMongoWrapperFromPrefs() {
        // open prefs
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        // get strings from prefs
        String server = settings.getString(Constants.PrefName.MongoDBServerPrefName, "localhost");
        String serverPort = settings.getString(Constants.PrefName.MongoDBServerPortPrefName,"12345");
        String dbname = settings.getString(Constants.PrefName.MongoDBDatabasePrefName,"db");
        String mongoUsername = settings.getString(Constants.PrefName.MongoDBUsernamePrefName,"username");
        String mongoPassword = settings.getString(Constants.PrefName.MongoDBPasswordPrefName,"password");
        String mongoCollection = settings.getString(Constants.PrefName.MongoDBCollectionPrefName, "entries");

        mMongoWrapper.updateURI(server, serverPort, dbname, mongoUsername, mongoPassword, mongoCollection);
    }

    // Note this is called from our local (background) message handler,
    // and also from APSLogic whenever it wants to update its own value.
    // This is intended to keep Android stuff out of APSLogic
    public void updateFromPersonalPrefs() {
        // get CAR value from prefs
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        double car = (double)settings.getFloat(Constants.PrefName.CARPrefName, (float) 30.0);
        // Notify APSLogic of new value
        mAPSLogic.setCAR(car);
        mAPSLogic.setMaxTempBasalRate((double)settings.getFloat(Constants.PrefName.PPMaxTempBasalRatePrefName,(float)6.1));
        mAPSLogic.setBGMin((double)settings.getFloat(Constants.PrefName.PPBGMinPrefName, (float) 95.0));
        mAPSLogic.setTargetBG((double)settings.getFloat(Constants.PrefName.PPTargetBGPrefName, (float) 115.0));
        mAPSLogic.setBGMax((double)settings.getFloat(Constants.PrefName.PPBGMaxPrefName, (float) 125.0));
    }

    // These functions are called by APSLogic.
    // This allows us to separate Android from APSLogic
    public void sendTempBasalToUI(TempBasalPair pair) {
        Intent intent = new Intent(Intents.APSLOGIC_TEMPBASAL_UPDATE);
        intent.putExtra("name", Constants.ParcelName.TempBasalPairParcelName);
        intent.putExtra(Constants.ParcelName.TempBasalPairParcelName, new TempBasalPairParcel(pair));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
    public void sendCurrBasalToUI(double basalRate) {
        Intent intent = new Intent(Intents.APSLOGIC_CURRBASAL_UPDATE).putExtra("value", basalRate);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void sendPredBGToUI(double predictedBG) {
        Intent intent = new Intent(Intents.APSLOGIC_PREDBG_UPDATE).putExtra("value",predictedBG);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void sendIOBToUI(double iobTotal) {
        Intent intent = new Intent(Intents.APSLOGIC_IOB_UPDATE).putExtra("value",iobTotal);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void sendCOBToUI(double cobTotal) {
        Intent intent = new Intent(Intents.APSLOGIC_COB_UPDATE).putExtra("value",cobTotal);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        if (mInstance == null) {
            mInstance = this;
        }

        /* This function runs in the main thread (UI thread) */
        /* Here is where we do some initialization, but no work */
        Log.d(TAG, "onCreate()");

        // make a thread to do our background work
        HandlerThread thread = new HandlerThread("Service Thread Name",
                Process.THREAD_PRIORITY_FOREGROUND);

        thread.start();
        // this doesn't work: dalvikvm reports threadid is 11, but we get -1.
        Log.d(TAG, "Started thread with thread id " + thread.getThreadId());
        Log.d(TAG, "My id is " + getMainLooper().getThread().getId());

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mTimerHandler = new Handler(); // does this need to be kept separate from mServiceHandler?


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
        mPumpManager = new PumpManager(getApplicationContext());
        mPumpManager.open();
        mAPSLogic = new APSLogic();
        mMongoWrapper = new MongoWrapper();
        updateMongoWrapperFromPrefs();

        //llog("End of onCreate()");
        llog("Roundtrip ready.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* This function runs in the Foreground */
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        /*
        simply deliver a message of work to be done.
        */
        if (intent != null) {
            // for Long operations (tasks involving stick or pump)
            // Put a task in the service handler's queue.
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.arg2 = intent.getIntExtra("what",0);
            msg.obj = intent.getStringExtra("something");
            // todo: fix hack
            if (msg.arg2 == Constants.SRQ.SET_SERIAL_NUMBER) {
                msg.obj = intent.getByteArrayExtra("serialNumber");
                //Log.w(TAG,"gui thread wrote intent arg2=what=SRQ.SET_SERIAL_NUMBER, obj=serialNumber=" + ByteUtil.shortHexString((byte[])msg.obj));
            } else if (msg.arg2 == Constants.SRQ.SET_TEMP_BASAL) {
                msg.obj = intent.getParcelableExtra(Constants.ParcelName.TempBasalPairParcelName);
                /*  The long way? :
                if (intent.hasExtra("name")) {
                    String name = intent.getStringExtra("name");
                    if (intent.hasExtra(name)) {
                        if (name == Constants.ParcelName.PumpSettingsParcelName) {
                            Bundle data = intent.getExtras();
                            PumpSettingsParcel p = data.getParcelable(name);
                            // do something with it.
                            receivePumpSettingsParcel(p);
                        }
                    }
                }
                */
            }
            mServiceHandler.sendMessage(msg);
        }

        // START_STICKY means don't kill me, let me run (someone will be using me)
        return START_STICKY;
    }

    protected void serviceMain() {
        // APSLOGIC_STARTUP requests the APSLogic module to do the
        // initial data collection, which can take a long time (MongoDB access, pump access)
        // get latest BG reading from Mongo

        broadcastAPSLogicStatusMessage("Accessing MongoDB for latest BG reading");
        MongoWrapper.BGReadingResponse bgResponse = mMongoWrapper.getBGReading();
        BGReading reading = bgResponse.reading;
        if (bgResponse.error) {
            broadcastAPSLogicStatusMessage("Error reading BG from mongo:" + bgResponse.errorMessage);
            Log.e(TAG,
                    String.format("Error reading BG from Mongo: %s, and BG reading reports %.2f at %s",
                            bgResponse.errorMessage,
                            reading.mBg,reading.mTimestamp.toLocalDateTime().toString()));
        }
        // Are the contents of BGReading reading "ok to use", even with an error?
        // how else to handle?

        // TODO: Need to make RTDemoService regularly hit the mongodb (every 5 min)
        // For now, the testdb button gets a reading.
        // broadcast the reading to the world. (esp. to MonitorActivity)
        Intent intent = new Intent(Intents.ROUNDTRIP_BG_READING);
        intent.putExtra("name", Constants.ParcelName.BGReadingParcelName);
        intent.putExtra(Constants.ParcelName.BGReadingParcelName, new BGReadingParcel(reading));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        Log.i(TAG, "Sending latest BG reading");
        broadcastAPSLogicStatusMessage(String.format("Latest BG reading reports %.2f at %s",
                reading.mBg,reading.mTimestamp.toLocalDateTime().toString()));

        mAPSLogic.updateCachedLatestBGReading(reading);

        // the above should be (re)moved.
        mAPSLogic.runAPSLogicOnce();
    }

    protected void serviceRepeat() {
        // have ourselves call us back in 5 minutes,
        startServiceRepeatTimer();
        // and post an intent to run it now. (So that it is run from the correct handler).
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.APSLOGIC_STARTUP);
        startService(intent);
    }

    protected void startServiceRepeatTimer() {
        // call serviceMain, 5 minutes from now.
        // often, but not always, mTimerRunnable==this
        mTimerHandler.postDelayed(mTimerRunnable, secondsBetweenRuns * 1000);
    }

    protected void stopServiceRepeatTimer() {
        // kill any running timer.
        mTimerHandler.removeCallbacksAndMessages(null);
    }

    protected void suspendServiceRepeatTimer(int delayMinutes) {
        // kill running timer.
        stopServiceRepeatTimer();
        int delayMilliseconds = delayMinutes * 60 * 1000;
        // call serviceRepeat after the given delay
        mTimerHandler.postDelayed(mTimerRunnable,delayMilliseconds);
    }

    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            serviceRepeat();
        }
    }

    private void showNotification(){
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
        mNM.notify(NOTIFICATION,notification);
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
        /* This function runs in the Foreground */
        /* release resources */
        mNM.cancel(NOTIFICATION);
        // tell user we've stopped:
        Toast.makeText(this,R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }
    // getting permission for the device is necessarily an asynchronous action
    boolean getCarelinkPermission() {
        UsbDevice device = getCarelinkDevice();
        int loopcount = 0;
        if (getUsbManager().hasPermission(device)) {
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

        // wait for permission (BLOCKING!)
        while (!getUsbManager().hasPermission(device)) {
            if (loopcount % 100 == 0) {
                Log.i("gapp","Waiting for Carelink Permission");
            }
            // sleep for milliseconds
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                // whatever
                Log.i("gapp","Exception(?):" + e.getMessage());
            }
            loopcount ++;
        }
        // receiver no longer needed?
        unregisterReceiver(mUsbReceiver);
        return true;
    }

    static UsbManager mUsbManager; // use getUsbManager() to get access
    UsbManager getUsbManager() {
        if (mUsbManager != null) {
            return mUsbManager;
        }
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        return mUsbManager;
    }

    static UsbDevice mCarelinkDevice; // use getCarelinkDevice to get access
    UsbDevice getCarelinkDevice() {
        // if we already have one, return it
        if (mCarelinkDevice != null) {
            return mCarelinkDevice;
        }

        // else, try to go get it.
        HashMap<String, UsbDevice> deviceList = getUsbManager().getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        UsbDevice device = null;
        while(deviceIterator.hasNext()){
            device = deviceIterator.next();
            if (deviceIsCarelink(device)) {
                break;
            } else {
                device = null;
            }
        }

        mCarelinkDevice = device;
        return mCarelinkDevice; // NOTE: may still be null, if we couldn't find it!
    }

}
