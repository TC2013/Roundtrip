package com.gxwtech.rtdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gxwtech.rtdemo.Services.RTDemoService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";

    // For receiving and displaying log messages from the Service thread
    BroadcastReceiver broadcastReceiver;
    int nRecentMessages = 50;
    List<String> msgList = new ArrayList<>();
    ArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, msgList);
        ListView lv = (ListView) findViewById(R.id.listView_Log);
        lv.setAdapter(adapter);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == Intents.ROUNDTRIP_STATUS_MESSAGE) {
                    Log.d(TAG,"Received Roundtrip_Status_message");
                    if (intent.hasExtra("messages")) {
                        ArrayList<String> newMsgList = intent.getStringArrayListExtra("messages");
                                Log.w(TAG, String.format("Found extra: %d messages",msgList.size()));
                        adapter.clear();
                        adapter.addAll(newMsgList);
                        adapter.notifyDataSetChanged();
                    }
                    if (intent.hasExtra(Intents.ROUNDTRIP_STATUS_MESSAGE_STRING)) {
                        String s = intent.getStringExtra(Intents.ROUNDTRIP_STATUS_MESSAGE_STRING);
                        Log.w(TAG,"Found extra: one string:" + s);
                    }

                } else if (intent.getAction() == Intents.ROUNDTRIP_TASK_RESPONSE) {
                    // pump settings viewer used to be here.
                    // I'm leaving it as an example of how to receive task_response
                    /*
                    if (intent.hasExtra("name")) {
                        String name = intent.getStringExtra("name");
                        if (intent.hasExtra(name)) {
                            if (intent.getAction() == Constants.ParcelName.PumpSettingsParcelName) {
                                Bundle data = intent.getExtras();
                                PumpSettingsParcel p = data.getParcelable(name);
                                // do something with it.
                                receivePumpSettingsParcel(p);
                            }
                        }
                    }
                    */
                }
            }
        };
        // the source of our null intents?
        startService(new Intent(this, RTDemoService.class).putExtra("srq", Constants.SRQ.START_SERVICE));
    }

    public void verifyPumpCommunications(View view) {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.VERIFY_PUMP_COMMUNICATIONS);
        startService(intent);
    }

    public void launchRTDemoSettingsActivity(View view) {
        Intent intent = new Intent(this,RTDemoSettingsActivity.class);
        startActivity(intent);
    }

    public void launchMonitorActivity(View view) {
        Intent intent = new Intent(this,MonitorActivity.class);
        startActivity(intent);
    }

    // No need to call stopRTService, as we don't care to ever stop the service.
    public void stopRTService(View view) {
        stopService(new Intent(this, RTDemoService.class));
    }

    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intents.ROUNDTRIP_STATUS_MESSAGE);
        intentFilter.addAction(Intents.ROUNDTRIP_TASK_RESPONSE);

        // register our desire to receive broadcasts from RTDemoService
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(broadcastReceiver, intentFilter);
    }

    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
