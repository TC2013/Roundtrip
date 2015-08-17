package com.gxwtech.rtdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gxwtech.rtdemo.services.RTDemoService;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import java.util.ArrayList;


public class MonitorActivity extends ActionBarActivity {
    private static final String TAG = "MonitorActivity";
    private final int MaxLogSize = 500;
    BroadcastReceiver mBroadcastReceiver;
    PreferenceBackedStorage mStorage;
    //    DateTime mLastBGUpdateTime = null;
    DateTime mSleepNotificationStartTime = null;
    int mSleepNotificationDuration = 0;
    ArrayList<String> msgList = new ArrayList<>();
    ArrayAdapter<String> adapter = null;
    // for periodically updating gui
    Handler timerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timerHandler = new Handler();
        mStorage = new PreferenceBackedStorage(getApplicationContext());
        setContentView(R.layout.activity_monitor);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, msgList);
        ListView lv = (ListView) findViewById(R.id.listView_MonitorMsgs);
        lv.setAdapter(adapter);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intents.ROUNDTRIP_BG_READING)) {
                    // Used to be, this passed the whole thing to the gui.
                    // Now we just get it from shared-prefs
                    UpdateBGReading();
                } else if (intent.getAction().equals(Intents.APSLOGIC_LOG_MESSAGE)) {
                    String msg = intent.getStringExtra("message");
                    receiveLogMessage(msg);
                } else if (intent.getAction().equals(Intents.ROUNDTRIP_SLEEP_MESSAGE)) {
                    int durationSeconds = intent.getIntExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION, 0);
                    Log.d(TAG, String.format("Received Sleep Notification: %d seconds", durationSeconds));
                    mSleepNotificationDuration = durationSeconds;
                    setSleepNotification();
                } else if (intent.getAction().equals(Intents.MONITOR_DATA_CHANGED)) {
                    updateGUIValues();
                }

            }
        };
    }

    public void receiveLogMessage(String msg) {
        // keep 50 messages?  make configurable?
        if (msg == null) {
            msg = "(null message)";
        }
        if (msg.equals("")) {
            msg = "(empty message)";
        }
        adapter.insert(msg, 0);
        if (adapter.getCount() > MaxLogSize) {
            adapter.remove(adapter.getItem(adapter.getCount() - 1));
        }
    }

    public void UpdateTextViewInt(int textView_id, String format, int value) {
        ((TextView) findViewById(textView_id)).setText(String.format(format, value));
    }

    public void UpdateTextViewDouble(int textView_id, String format, double value) {
        ((TextView) findViewById(textView_id)).setText(String.format(format, value));
    }

    public void UpdateBGReading() {
        UpdateTextViewInt(R.id.textView_LatestBG, "%d mg/dL", (int) mStorage.getLatestBGReading().mBg);
    }

    public void updateBGTimer() {
        DateTime lastBGUpdateTime = mStorage.getLatestBGReading().mTimestamp;
        int elapsedMinutes = Minutes.minutesBetween(lastBGUpdateTime, DateTime.now()).getMinutes();
        String viewtext = "never";
        int textColor = Color.rgb(200, 0, 0); // red
        TextView view = (TextView) findViewById(R.id.textView_LastBGReadTime);
        if (elapsedMinutes < 1000) {
            viewtext = String.format("%d min ago", elapsedMinutes);
            if ((elapsedMinutes > 10) || (elapsedMinutes < 1)) {
                textColor = Color.rgb(200, 0, 0); // red
            } else {
                textColor = Color.rgb(0, 0, 0); // black
            }
        }
        view.setText(viewtext);
        view.setTextColor(textColor);
    }

    public void updateCurrentIOB_TextView() {
        UpdateTextViewDouble(R.id.textView_IOB, "%.1f U", mStorage.monitorIOB.get());
    }

    public void updateCurrentCOB_TextView() {
        UpdateTextViewDouble(R.id.textView_COB, "%.1f gm", mStorage.monitorCOB.get());
    }

    public void updateCurrentBasal_TextView() {
        UpdateTextViewDouble(R.id.textView_CurrentBasal, "%.3f U/hr", mStorage.monitorCurrBasalRate.get());
    }

    public void updatePredictedBG_TextView() {
        UpdateTextViewDouble(R.id.textView_PredBG, "%.1f mg/dL", mStorage.monitorPredictedBG.get());
    }

    public void updateTempBasal_TextView() {
        UpdateTextViewDouble(R.id.textView_TempBasalRate, "%.3f U/hr", mStorage.monitorTempBasalRate.get());
        UpdateTextViewInt(R.id.textView_TempBasalMinRemaining, "%d min", mStorage.monitorTempBasalDuration.get());
    }

    protected void setSleepNotification() {
        mSleepNotificationStartTime = DateTime.now();
        TextView tv = (TextView) findViewById(R.id.textView_SleepNotification);
        updateSleepNotification();
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
        Intent intent = new Intent(this, RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.START_REPEAT_ALARM);
        startService(intent);
    }

    public void buttonSuspendClicked(View view) {
        // need a dialog for the options:
        Intent intent = new Intent(this, SuspendAPSActivity.class);
        startActivity(intent);
    }

    public void buttonStopClicked(View view) {
        Intent intent = new Intent(this, RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.STOP_REPEAT_ALARM);
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

    protected void updateGUIValues() {
        UpdateBGReading();
        updateBGTimer();
        updateCurrentIOB_TextView();
        updateCurrentCOB_TextView();
        updateCurrentBasal_TextView();
        updatePredictedBG_TextView();
        updateTempBasal_TextView();
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
        intentFilter.addAction(Intents.MONITOR_DATA_CHANGED);
        // register our desire to receive broadcasts from RTDemoService
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver, intentFilter);
        updateGUIValues();
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
