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

import com.gxwtech.rtdemo.Carelink.util.ByteUtil;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.MainActivity;
import com.gxwtech.rtdemo.MongoWrapper;
import com.gxwtech.rtdemo.R;
import com.gxwtech.rtdemo.Services.PumpManager.PumpManager;
import com.gxwtech.rtdemo.Services.PumpManager.PumpSettingsParcel;

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
 */


public class RTDemoService extends Service {
    private static final String TAG = "RTDemoService";

    protected static RTDemoService mInstance = null;
    NotificationManager mNM;
    Looper mServiceLooper;
    ServiceHandler mServiceHandler = null;
    private int NOTIFICATION = R.string.local_service_started;

    protected int secondsBetweenRuns = 20;
    protected int maxQueueLength = 100;
    protected BlockingDeque<String> messageQ = new LinkedBlockingDeque<>(maxQueueLength);

    // Intent for use in asking permission to use Carelink stick
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    PumpManager mPumpManager;

    public static RTDemoService getInstance() {
        return mInstance;
    }

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
                        mPumpManager.close();
                        mCarelinkDevice = null; // whack it, to force reloading
                        mUsbManager = null;  // whack it, to force reloading
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

            } else if (msg.arg2 == Constants.SRQ.WAKE_CARELINK) {
                // this command should be the first place
                // we actually try to talk over USB to the carelink.
                if (!mPumpManager.wakeUpCarelink()) {
                    llog("Error accessing Carelink USB Stick.");
                    //Log.e(TAG,"wakeUpCarelink failed");
                } else {
                    llog("Carelink ready.");
                }
            } else if (msg.arg2 == Constants.SRQ.VERIFY_PUMP_COMMUNICATIONS) {
                if (!mPumpManager.verifyPumpCommunications()) {
                    llog("Error accessing pump.");
                } else {
                    llog("Pump ready.");
                }
            } else if (msg.arg2 == Constants.SRQ.SET_SERIAL_NUMBER) {
                // ewww....
                byte[] serialNumber = (byte[])msg.obj;
                Log.w(TAG,"Service received set serial number, serialNumber=" + ByteUtil.shortHexString(serialNumber));
                mPumpManager.setSerialNumber(serialNumber);
            } else if (msg.arg2 == Constants.SRQ.REPORT_PUMP_SETTINGS) {
                PumpSettingsParcel parcel = new PumpSettingsParcel();
                parcel.initFromPumpSettings(mPumpManager.getPumpSettings());
                sendTaskResponseParcel(parcel, "PumpSettingsParcel");
            } else if (msg.arg2 == Constants.SRQ.VERIFY_DB_ACCESS) {
                // get latest BG reading from Mongo
                llog("Accessing MongoDB for latest BG");
                MongoWrapper mongoWrapper = new MongoWrapper();
                llog(mongoWrapper.doit());

            } else {
                // just wait 5 seconds
                long endTime = System.currentTimeMillis() + 5*1000;
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

    protected void sendTaskResponseParcel(Parcelable p, String typename) {
        // send back to the UI thread an arbitrary response parcel
        Intent intent = new Intent(Intents.ROUNDTRIP_TASK_RESPONSE);
        intent.putExtra("name",typename);
        intent.putExtra(typename,p);
        Log.d(TAG,"Sending task response parcel, name = " + typename);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

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
                .putExtra(Intents.ROUNDTRIP_STATUS_MESSAGE_STRING,msg);
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
                Log.w(TAG,"gui thread wrote intent arg2=what=SRQ.SET_SERIAL_NUMBER, obj=serialNumber=" + ByteUtil.shortHexString((byte[])msg.obj));
            }
            mServiceHandler.sendMessage(msg);
        }

        // START_STICKY means don't kill me, let me run (someone will be using me)
        return START_STICKY;
    }

    protected void serviceMain() {
    }

    protected void serviceRepeat() {
        serviceMain();
        setNextRunTimer(secondsBetweenRuns * 1000);
    }

    protected void setNextRunTimer(int nMilliseconds) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + nMilliseconds,
                PendingIntent.getService(this, 0, new Intent(this, RTDemoService.class),
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT));

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
