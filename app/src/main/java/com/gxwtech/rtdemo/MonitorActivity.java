package com.gxwtech.rtdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gxwtech.rtdemo.Services.RTDemoService;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.ArrayList;


public class MonitorActivity extends ActionBarActivity {
    private static final String TAG = "MonitorActivity";
    BroadcastReceiver mBroadcastReceiver;
    DateTime mLastBGUpdateTime = null;
    DateTime mSleepNotificationStartTime = null;
    int mSleepNotificationDuration = 0;
    ArrayList<String> mMessageLog = new ArrayList<>();
    ArrayAdapter<String> adapter = null;

    // for periodically updating gui
    Handler timerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timerHandler = new Handler();
        setContentView(R.layout.activity_monitor);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == Intents.ROUNDTRIP_BG_READING) {
                    Log.d(TAG, "Received BG Reading");
                    if (intent.hasExtra("name")) {
                        String name = intent.getStringExtra("name");
                        if (intent.hasExtra(name)) {
                            if (name == Constants.ParcelName.BGReadingParcelName) {
                                Bundle data = intent.getExtras();
                                BGReadingParcel p = data.getParcelable(name);
                                // do something with it.
                                UpdateBGReading(p);
                            }
                        }
                    }
                } else if (intent.getAction() == Intents.APSLOGIC_LOG_MESSAGE) {
                    String msg = intent.getStringExtra("message");
                    receiveLogMessage(msg);
                } else if (intent.getAction() == Intents.ROUNDTRIP_SLEEP_MESSAGE) {
                    int durationSeconds = intent.getIntExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION,0);
                    Log.d(TAG,String.format("Received Sleep Notification: %d seconds",durationSeconds));
                    setSleepNotification(durationSeconds);
                }

            }
        };
    }

    private int MaxLogSize = 50;
    public void receiveLogMessage(String msg) {
        // keep 50 messages?  make configurable?
        mMessageLog.add(0,msg);
        if (mMessageLog.size() > MaxLogSize) {
            mMessageLog.remove(mMessageLog.size()-1);
        }
        rebuildArrayAdapter(); // do we need to do this?
    }

    public void rebuildArrayAdapter() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mMessageLog);
        ListView lv = (ListView) findViewById(R.id.listView_MonitorMsgs);
        lv.setAdapter(adapter);
    }

    public void UpdateBGReading(BGReading bgr) {
        TextView viewBG = (TextView)findViewById(R.id.textView_LatestBG);
        String bgtext = String.format("%d mg/dL",((int)bgr.mBg));
        viewBG.setText(bgtext);
        mLastBGUpdateTime = bgr.mTimestamp;
        updateBGTimer();
    }

    public void updateBGTimer() {
        if (mLastBGUpdateTime != null) {
            Seconds seconds = Seconds.secondsBetween(mLastBGUpdateTime, DateTime.now());
            int elapsedMinutes = seconds.getSeconds() / 60;
            TextView view = (TextView) findViewById(R.id.textView_LastBGReadTime);
            view.setText(String.format("%d min ago", elapsedMinutes));
        }
    }

    protected void setSleepNotification(int durationSeconds) {
        mSleepNotificationStartTime = DateTime.now();
        mSleepNotificationDuration = durationSeconds;
        TextView tv = (TextView)findViewById(R.id.textView_SleepNotification);
        String note = String.format("zzZ %d",durationSeconds);
        tv.setText(note);
        tv.setVisibility(View.VISIBLE);
    }

    protected void updateSleepNotification() {
        if (mSleepNotificationStartTime != null) {
            Seconds s = Seconds.secondsBetween(mSleepNotificationStartTime, DateTime.now());
            int secondsRemaining = mSleepNotificationDuration - s.getSeconds();
            TextView tv = (TextView) findViewById(R.id.textView_SleepNotification);
            if (secondsRemaining < 0) {
                tv.setVisibility(View.INVISIBLE);
                // cancel updates
                mSleepNotificationStartTime = null;
            } else {
                String note = String.format("zzZ %d", secondsRemaining);
                tv.setText(note);
            }
        }
    }

    public void startupButtonClicked(View view) {
        Log.d(TAG, "startupButtonClicked");
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.APSLOGIC_STARTUP);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onResume() {
        super.onResume();
        // fixme: do we need to rebuild our display from a bundle?
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intents.ROUNDTRIP_STATUS_MESSAGE);
        //intentFilter.addAction(Intents.ROUNDTRIP_TASK_RESPONSE);
        intentFilter.addAction(Intents.ROUNDTRIP_BG_READING);
        intentFilter.addAction(Intents.APSLOGIC_LOG_MESSAGE);
        intentFilter.addAction(Intents.ROUNDTRIP_SLEEP_MESSAGE);

        // register our desire to receive broadcasts from RTDemoService
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver, intentFilter);
        rebuildArrayAdapter();
    }

    protected void onPause() {
        super.onPause();
        // fixme: Do we need to save our display messages to a bundle?
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        // run this one second from now
        timerHandler.postDelayed(new TimerRunnable(), 1000);
    }

    @Override
    public void onStop() {
        super.onStop();
        timerHandler.removeCallbacksAndMessages(null);
    }

    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            updateBGTimer();
            updateSleepNotification();
            // We post this once per second
            timerHandler.postDelayed(this, 1000);
        }
    }

}
