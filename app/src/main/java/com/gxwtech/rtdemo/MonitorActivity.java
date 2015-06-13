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

import com.gxwtech.rtdemo.Medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.Services.PumpManager.TempBasalPairParcel;
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

    double m_iob = -99.0;
    double m_cob = -99.0;
    double m_cb = -99.0;
    double m_pbg = -99.0;
    TempBasalPairParcel m_pair = new TempBasalPairParcel();
    BGReadingParcel m_p = new BGReadingParcel();



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
                                m_p = p;
                                // do something with it.
                                UpdateBGReading();
                            }
                        }
                    }
                } else if (intent.getAction() == Intents.APSLOGIC_LOG_MESSAGE) {
                    String msg = intent.getStringExtra("message");
                    receiveLogMessage(msg);
                } else if (intent.getAction() == Intents.ROUNDTRIP_SLEEP_MESSAGE) {
                    int durationSeconds = intent.getIntExtra(Intents.ROUNDTRIP_SLEEP_MESSAGE_DURATION,0);
                    Log.d(TAG,String.format("Received Sleep Notification: %d seconds",durationSeconds));
                    mSleepNotificationDuration = durationSeconds;
                    setSleepNotification();
                } else if (intent.getAction() == Intents.APSLOGIC_IOB_UPDATE) {
                    double iob = intent.getDoubleExtra("value",-98.0);
                    m_iob = iob;
                    updateCurrentIOB_TextView();
                } else if (intent.getAction() == Intents.APSLOGIC_COB_UPDATE) {
                    double cob = intent.getDoubleExtra("value",-98.0);
                    m_cob = cob;
                    updateCurrentCOB_TextView();
                } else if (intent.getAction() == Intents.APSLOGIC_CURRBASAL_UPDATE) {
                    double cb = intent.getDoubleExtra("value",-98.0);
                    m_cb = cb;
                    updateCurrentBasal_TextView();
                } else if (intent.getAction() == Intents.APSLOGIC_PREDBG_UPDATE) {
                    double pbg = intent.getDoubleExtra("value",-98.0);
                    m_pbg = pbg;
                    updatePredictedBG_TextView();
                } else if (intent.getAction() == Intents.APSLOGIC_TEMPBASAL_UPDATE) {
                    Bundle data = intent.getExtras();
                    TempBasalPairParcel pair = data.getParcelable(Constants.ParcelName.TempBasalPairParcelName);
                    m_pair = pair;
                    updateTempBasal_TextView();
                }
            }
        };
    }

    private int MaxLogSize = 500;
    public void receiveLogMessage(String msg) {
        // keep 50 messages?  make configurable?
        if (msg == null) {
            msg = "(null message)";
        }
        if (msg.equals("")) {
            msg = "(empty message)";
        }
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

    public void UpdateBGReading() {
        TextView viewBG = (TextView)findViewById(R.id.textView_LatestBG);
        String bgtext = String.format("%d mg/dL",((int)m_p.mBg));
        viewBG.setText(bgtext);
        mLastBGUpdateTime = m_p.mTimestamp;
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

    public void updateCurrentIOB_TextView() {
        TextView textView = (TextView) findViewById(R.id.textView_IOB);
        String str = String.format("%.1f U",m_iob);
        textView.setText(str);
    }

    public void updateCurrentCOB_TextView() {
        TextView textView = (TextView) findViewById(R.id.textView_COB);
        String str = String.format("%.1f gm",m_cob);
        textView.setText(str);
    }

    public void updateCurrentBasal_TextView() {
        TextView textView = (TextView) findViewById(R.id.textView_CurrentBasal);
        String str = String.format("%.3f U/hr",m_cb);
        textView.setText(str);
    }

    public void updatePredictedBG_TextView() {
        TextView textView = (TextView) findViewById(R.id.textView_PredBG);
        String str = String.format("%.1f mg/dL",m_pbg);
        textView.setText(str);
    }
    public void updateTempBasal_TextView() {
        TextView textView_rate = (TextView) findViewById(R.id.textView_TempBasalRate);
        String rateString = String.format("%.3f U/hr",m_pair.mInsulinRate);
        textView_rate.setText(rateString);

        TextView textView_minRemaining = (TextView) findViewById(R.id.textView_TempBasalMinRemaining);
        String minRemainingString = String.format("%d min",m_pair.mDurationMinutes);
        textView_minRemaining.setText(minRemainingString);
    }

    protected void setSleepNotification() {
        mSleepNotificationStartTime = DateTime.now();
        TextView tv = (TextView)findViewById(R.id.textView_SleepNotification);
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
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.START_AUTO_MODE);
        startService(intent);
    }

    public void buttonSuspendClicked(View view) {
        // need a dialog for the options:
        Intent intent = new Intent(this,SuspendAPSActivity.class);
        startActivity(intent);
    }

    public void buttonStopClicked(View view) {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.STOP_AUTO_MODE);
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
        intentFilter.addAction(Intents.APSLOGIC_IOB_UPDATE);
        intentFilter.addAction(Intents.APSLOGIC_COB_UPDATE);
        intentFilter.addAction(Intents.APSLOGIC_CURRBASAL_UPDATE);
        intentFilter.addAction(Intents.APSLOGIC_PREDBG_UPDATE);
        intentFilter.addAction(Intents.APSLOGIC_TEMPBASAL_UPDATE);
        // register our desire to receive broadcasts from RTDemoService
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver, intentFilter);
        rebuildArrayAdapter();
        UpdateBGReading();
        updateBGTimer();
        updateCurrentIOB_TextView();
        updateCurrentCOB_TextView();
        updateCurrentBasal_TextView();
        updatePredictedBG_TextView();
        updateTempBasal_TextView();
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
