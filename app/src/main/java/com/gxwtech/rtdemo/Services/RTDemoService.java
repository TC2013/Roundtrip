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
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.IccOpenLogicalChannelResponse;
import android.util.Log;
import android.widget.Toast;

import com.gxwtech.rtdemo.CarelinkStick.DeviceTransport;
import com.gxwtech.rtdemo.CarelinkStick.NightscoutDeviceTransport;
import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.MainActivity;
import com.gxwtech.rtdemo.MongoWrapper;
import com.gxwtech.rtdemo.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.hoho.android.usbserial.driver.*;
import com.hoho.android.usbserial.util.HexDump;

/**
 * Created by geoff on 4/9/15.
 */


public class RTDemoService extends Service {

    protected static RTDemoService mInstance = null;
    NotificationManager mNM;
    Looper mServiceLooper;
    ServiceHandler mServiceHandler = null;
    private int NOTIFICATION = R.string.local_service_started;

    // Tag for logging messages (makes 'em easy to see in LOGCAT)
    private String TAG = "GGWRTD";
    protected int secondsBetweenRuns = 20;
    protected int maxQueueLength = 100;
    protected BlockingDeque<String> messageQ = new LinkedBlockingDeque<>(maxQueueLength);

    // our cached connection to the USB system manager
    UsbManager mUsbManager;
    // Intent for use in asking permission to use Carelink stick
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    // don't use directly.  Use getCarelinkDevice()
    private UsbDevice mCarelinkDevice;

    // for the moment
    UsbSerialDriver mDriver = null;
//    UsbSerialPort mPort = null;
    UsbDeviceConnection mConnection = null;
    DeviceTransport mTransport = null;

    public static RTDemoService getInstance() {
        return mInstance;
    }

    boolean deviceIsCarelink(UsbDevice device) {
        if (device == null) return false;
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
                    } else {
                        llog("USB device disconnected (not carelink):" + device.toString());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (deviceIsCarelink(device)) {
                    if (!mUsbManager.hasPermission(device)) {
                        llog("Carelink device attached, permission OK.");
                    } else {
                        llog("Carelink device attached, permission NOT GRANTED.");
                    }
                } else {
                    llog("Other USB device attached:" + device.toString());
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
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            int startId = msg.arg1;
            //llog("handleMessage: received msg, arg1="+startId);
            Object someObject = msg.obj;

            if (msg.arg2 == 1337) {
                UsbSerialProber prober = UsbSerialProber.getDefaultProber();
                List<UsbSerialDriver> dlist = prober.findAllDrivers(mUsbManager);

                llog(String.format("Prober returned %d drivers", dlist.size()));
                if (dlist.size() > 0) {
                    for (int i = 0; i < dlist.size(); i++) {
                        llog(String.format("Driver %d: %s", i, dlist.get(i).toString()));
                    }
                }

// Set up permissions for carelink
                // this BLOCKS until we get permission!
                // since it blocks, we can't do it in Create();
                getCarelinkPermission();

                if (dlist.size() > 0) {
                    mDriver = dlist.get(0);
                }
                mConnection = mUsbManager.openDevice(mDriver.getDevice());
                if (mConnection == null) {
                    llog("Device connection is NULL");
                }

                // We got a connection. Hand the rest over to the Transport

                mTransport = new NightscoutDeviceTransport(mDriver,mConnection);

                // now test it.
                testPump(mTransport);

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

        // Set up connection to Carelink
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // create a PendingIntent to give to the USB Manager, to call us back with the result.
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // set up a filter for what broadcasts we wish to catch with mUsbReceiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // see EXTRA_DEVICE too
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        // set up the receiver with our filter
        registerReceiver(mUsbReceiver, filter);

        //llog("End of onCreate()");
        llog("Press Start to access MongoDB");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* This function runs in the Foreground */
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        /*
        simply deliver a message of work to be done.
        */
        if (intent != null) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.arg2 = intent.getIntExtra("arg2",0);
            msg.obj = intent.getStringExtra("something");
            mServiceHandler.sendMessage(msg);
        }

        // START_STICKY means don't kill me, let me run (someone will be using me)
        return START_STICKY;
    }


    UsbDevice getCarelinkDevice() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        // if we already have one, return it
        if (mCarelinkDevice != null) {
            return mCarelinkDevice;
        }

        // else, try to go get it.

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

    // getting permission for the device is necessarily an asynchronous action
    boolean getCarelinkPermission() {
        UsbDevice device = getCarelinkDevice();
        int loopcount = 0;
        if (mUsbManager.hasPermission(device)) {
            return true;
        }
        // ask for permission
        mUsbManager.requestPermission(device, mPermissionIntent);

        // wait for permission (BLOCKING!)
        while (!mUsbManager.hasPermission(device)) {
            if (loopcount % 100 == 0) {
                llog("Waiting for Carelink Permission");
            }
            // sleep for milliseconds
            try {
                Thread.sleep(100);
            } catch (java.lang.InterruptedException e) {
                // whatever
                llog("Exception(?):" + e.getMessage());
            }
            loopcount ++;
        }
        return true;
    }

    int querySignalStrength(DeviceTransport t) {
        llog("Query signal strength");
        int rval = 0;
        boolean finished = false;
        // this test packet reads the pump RTC
        byte[] testPacket = new byte[]
                {0x06, 0x00};
        try {
            t.open();
        } catch (IOException e) {
            llog("IOException in attempting to open DeviceTransport:" + e.getMessage());
        }

        try {
            int nWritten = t.write(testPacket, 100);
            llog(String.format("Wrote %d bytes: %s",nWritten,HexDump.dumpHexString(testPacket)));
        } catch (IOException e) {
            llog("IOException on test packet write: " + e.getMessage());
        }
        int nRead = -1;
        int tries = 0;
        while (!finished) {
            byte[] buf = new byte[64];
            try {
                nRead = t.read(buf, 1000);
            } catch (IOException e) {
                llog("IOException while reading test packet: " + e.getMessage());
                llog(String.format("(And nRead is %d)",nRead));
            }
            if (nRead < 1) {
                llog(String.format("Read failed, nRead=%d, tries %d",nRead,tries));
                tries = tries + 1;
                if (tries == 30) {
                    finished = true;
                }
            } else {
                finished = true;
                llog(String.format("Success: read %d bytes",nRead));
                llog("Dump:" + HexDump.dumpHexString(buf));
                // decode date string from pump:

            }
        }

        return rval;
    }

    void testPump(DeviceTransport t) {
        querySignalStrength(t);
        llog("Seeking pump RTC");
        boolean finished = false;
        // this test packet reads the pump RTC
        byte[] testPacket = new byte[] {0x01,0x00,(byte)0xa7,0x01,0x46,0x73,0x24,(byte)0x80,
                0x00,0x00,0x02,0x01,0x00,0x70,0x7c,0x00};

        process(t,testPacket);

        try {
            t.open();
        } catch (IOException e) {
            llog("IOException in attempting to open DeviceTransport:" + e.getMessage());
        }

        try {
            int nWritten = t.write(testPacket, 100);
            llog(String.format("Wrote %d bytes:",nWritten) + HexDump.dumpHexString(testPacket));
        } catch (IOException e) {
            llog("IOException on test packet write: " + e.getMessage());
        }
        int nRead = -1;
        int tries = 0;
        while (!finished) {
            byte[] buf = new byte[64];
            try {
                nRead = t.read(buf, 1000);
            } catch (IOException e) {
                llog("IOException while reading test packet: " + e.getMessage());
                llog(String.format("(And nRead is %d)",nRead));
            }
            if (nRead < 1) {
                llog(String.format("Read failed, nRead=%d, tries %d",nRead,tries));
                tries = tries + 1;
                if (tries == 30) {
                    finished = true;
                }
            } else {
                finished = true;
                llog(String.format("Success: read %d bytes",nRead));
                llog("Dump:" + HexDump.dumpHexString(buf));
                // decode date string from pump:

            }
        }

        // do it again, see what we get

        try {
            int nWritten = t.write(testPacket, 100);
            llog(String.format("Wrote %d bytes:",nWritten) + HexDump.dumpHexString(testPacket));
        } catch (IOException e) {
            llog("2IOException on test packet write: " + e.getMessage());
        }

        nRead = -1;
        tries = 0;
        finished = false;
        while (!finished) {
            byte[] buf = new byte[64];
            try {
                nRead = t.read(buf, 1000);
            } catch (IOException e) {
                llog("2IOException while reading test packet: " + e.getMessage());
                llog(String.format("(And nRead is %d)",nRead));
            }
            if (nRead < 1) {
                llog(String.format("2Read failed, nRead=%d, tries %d",nRead,tries));
                tries = tries + 1;
                if (tries == 30) {
                    finished = true;
                }
            } else {
                finished = true;
                llog(String.format("2Success: read %d bytes",nRead));
                llog("2Dump:" + HexDump.dumpHexString(buf));
                // decode date string from pump:

            }
        }
    }

    public void downloadPacket(DeviceTransport t,int size) {

    }

    public void process(DeviceTransport t, byte[] command) {
        byte[] raw;
        raw = send_force_read(t,command);
        if (raw.length == 0) {
            llog("process zero length read, try again");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                llog("Sleep interrupted:" + e.getMessage());
            }
            try {
                raw = t.read(64, 100);
            } catch (IOException e) {
                llog("Process read exception: " + e.getMessage());
            }
        }
        if (raw.length > 0) {
            llog(HexDump.dumpHexString(raw));
        }
    }

    void write_and_suck_dry (DeviceTransport t, byte[] command) {
        int i = 0;
        int nRead = 0;
        byte[] buf = new byte[64];
        try {
            t.write(command, 100);
        } catch (IOException e) {
            llog("IOException in read: " + e.getMessage());
        }
        while (i < 50) {
            i = i + 1;
            try {
                nRead = t.read(buf, 500);
            } catch (IOException e) {
                llog("IOException in read: " + e.getMessage());
            }
            llog(String.format("Read %d bytes",nRead));
            if (nRead > 0) {
                llog(HexDump.dumpHexString(buf));
            }
        }

    }

    // called from process, download_packet
    byte[] send_force_read(DeviceTransport t, byte[] byteCommand) {
        int retries = 5;
        int timeout = 1;
        byte[] command = byteCommand;
        int read_size = 64;
        int size = 64;  // should be the size of the command (see stick.py line 592)
        byte[] raw = new byte[] {};
        int i;
        int delay = 250; // millis
        for (i=0; i<retries; i++) {
            llog(String.format("Send force read %d/%d", (i+1),retries));
            try {
                int nWritten = t.write(command,timeout * 1000);
                llog(String.format("Wrote %d bytes:",nWritten) + HexDump.dumpHexString(command));
            } catch (IOException e) {
                llog("IOException in command write: " + e.getMessage());
            }
            try {
                llog("Sleeping 10 seconds");
                Thread.sleep(/*command.*/10000);
            } catch (InterruptedException e) {
                llog("Sleep interrupted: " + e.getMessage());
            }
            try {
                raw = t.read(size,250);
                llog(String.format("Readx %d bytes",raw.length));
                if (raw.length > 0) {
                    llog("\n" + HexDump.dumpHexString(raw));
                }
            } catch (IOException e) {
                llog("IOException in read: " + e.getMessage());
            }
            if (raw.length == 0) {
                llog("Zero length read, try once more, sleep 250 millis");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    llog("Sleep interrupted: " + e.getMessage());
                }
                try {
                    raw = t.read(size,250);
                    llog(String.format("Readx %d bytes",raw.length));
                    if (raw.length > 0) {
                        llog("\n" + HexDump.dumpHexString(raw));
                    }
                } catch (IOException e) {
                    llog("IOException in read: " + e.getMessage());
                }
                if (raw.length == 0) {
                    llog("Failed to download anything!");
                }
            }
        }
        return raw;
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


}
