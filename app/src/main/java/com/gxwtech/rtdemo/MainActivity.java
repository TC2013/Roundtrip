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

import com.gxwtech.rtdemo.Services.RTDemoService;

import java.util.List;


public class MainActivity extends ActionBarActivity {

    // For receiving and displaying log messags from the Service thread
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
                updateText();
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
            Log.i("RTDemoMain","Failed to get singleton instance.");

        }
    }

    // contains a View parameter because it is called from a control (a button)
    // No need to call startRTService, as it is called from onCreate()
    public void startRTService(View view) {
        // start the RTService thread
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("arg2",1337);
        startService(intent);
    }

    // No need to call stopRTService, as we don't care to ever stop the service.
    public void stopRTService(View view) {
        stopService(new Intent(this, RTDemoService.class));
    }

    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intents.ROUNDTRIP_STATUS_MESSAGE);


        // register our desire to receive broadcasts
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(broadcastReceiver, intentFilter);
        // update our log view from the current list of log messages in the service
        updateText();

        // launch the usb prober (for now)
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("arg2",1337);
        startService(intent);
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
