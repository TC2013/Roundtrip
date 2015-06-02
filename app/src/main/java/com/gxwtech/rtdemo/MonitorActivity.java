package com.gxwtech.rtdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    ArrayList<String> mMessageLog = new ArrayList<>();
    ArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                }
                if (intent.getAction() == Intents.APSLOGIC_LOG_MESSAGE) {
                    String msg = intent.getStringExtra("message");
                    receiveLogMessage(msg);
                }
            }
        };
    }

    private int MaxLogSize = 50;
    public void receiveLogMessage(String msg) {
        // keep 50 messages?  make configurable?
        mMessageLog.add(msg);
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
        Seconds seconds = Seconds.secondsBetween(DateTime.now(),mLastBGUpdateTime);
        int elapsedMinutes = seconds.getSeconds() / 60;
        TextView view = (TextView)findViewById(R.id.textView_LastBGReadTime);
        view.setText(String.format("%d min ago", elapsedMinutes));
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
        intentFilter.addAction(Intents.ROUNDTRIP_TASK_RESPONSE);
        intentFilter.addAction(Intents.ROUNDTRIP_BG_READING);
        intentFilter.addAction(Intents.APSLOGIC_LOG_MESSAGE);

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


}
