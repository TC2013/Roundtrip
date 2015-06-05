package com.gxwtech.rtdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.gxwtech.rtdemo.Carelink.util.ByteUtil;
import com.gxwtech.rtdemo.Services.PumpManager.PumpSettingsParcel;
import com.gxwtech.rtdemo.Services.RTDemoService;

import java.util.List;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";

    // For receiving and displaying log messages from the Service thread
    private ListView mLogWindow;
    BroadcastReceiver broadcastReceiver;
    int nRecentMessages = 20;
    List<String> msgList = null;
    ArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, RTDemoService.class));

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == Intents.ROUNDTRIP_STATUS_MESSAGE) {
                    updateText();
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

    }

    public void updateText() {
        RTDemoService svc = RTDemoService.getInstance();
        if (svc != null) {
            msgList = svc.getRecentMessages(nRecentMessages);
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, msgList);
            ListView lv = (ListView) findViewById(R.id.listView_Log);
            lv.setAdapter(adapter);
        } else {
            /*
            this can happen at the start of the app, when all is not yet ready.
             */
            Log.i("RTDemoMain","Failed to get singleton instance. (ignore)");

        }
    }

    // contains a View parameter because it is called from a control (a button)
    // No need to call startRTService, as it is called from onResume()
    public void startRTService(View view) {
        // start the RTService thread
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.START_SERVICE);
        startService(intent);
    }

    public void verifyPumpCommunications(View view) {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.VERIFY_PUMP_COMMUNICATIONS);
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
