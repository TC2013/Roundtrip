package com.gxwtech.rtdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gxwtech.rtdemo.services.pumpmanager.PumpSettingsParcel;
import com.gxwtech.rtdemo.services.RTDemoService;


public class PumpSettingsActivity extends ActionBarActivity {
    private final static String TAG = "PumpSettingsActivity";
    BroadcastReceiver mBroadcastReceiver;
    PumpSettingsParcel mPumpSettings;
    ArrayAdapter<String> mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPumpSettings = new PumpSettingsParcel();
        setContentView(R.layout.activity_pump_settings);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG,"Received broadcast: intent action is " + intent.getAction().toString());
                if (intent.getAction() == Intents.ROUNDTRIP_TASK_RESPONSE) {
                    Log.d(TAG,"Received task response");
                    if (intent.hasExtra("name")) {
                        String name = intent.getStringExtra("name");
                        Log.d(TAG,"Field 'name' is " + name);
                        if (intent.hasExtra(name)) {
                            if (name == Constants.ParcelName.PumpSettingsParcelName) {
                                Bundle data = intent.getExtras();
                                PumpSettingsParcel p = data.getParcelable(name);
                                // do something with it.
                                receivePumpSettingsParcel(p);
                            }
                        }
                    }
                }
            }
        };
        // If the pumpSerialNumber EditText is the first focused item,
        // it brings up the number entry keyboard.  So focus somewhere else.
        Button myBtn = (Button)findViewById(R.id.button_getPumpSettings);
        myBtn.setFocusableInTouchMode(true);
        myBtn.requestFocus();
    }

    // this is run when the SET button is clicked.
    public void editSerialNumberChanged(View view) {
        EditText esn = (EditText) findViewById(R.id.editText_pumpSerialNumber);
        String sn = esn.getText().toString();
        Log.w(TAG,"editSerialNumberChanged:" + sn);
        // save serial number in SharedPreferences
        SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
        SharedPreferences.Editor edit= preferences.edit();
        edit.putString(Constants.PrefName.SerialNumberPrefName, sn);
        edit.commit();
    }

    /*
    // set the 3 byte serial number for the pump
    public void setSerialNumber(String sn) {
        // now convert to a 3 byte string
        byte[] sn_bytes = HexDump.hexStringToByteArray(sn);
        //Log.w(TAG,"setSerialNumber bytes:" + HexDump.toHexString(sn_bytes));
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.SET_SERIAL_NUMBER);
        intent.putExtra("serialNumber", sn_bytes);
        startService(intent);
    }*/

    // get serial number from preferences, load it into proper field
    public String updateSerialNumberFromPreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        String serialNumber = settings.getString(Constants.PrefName.SerialNumberPrefName, "000000");
        EditText editText = (EditText)findViewById(R.id.editText_pumpSerialNumber);
        editText.setText(serialNumber);
        return serialNumber;
    }

    public void receivePumpSettingsParcel(PumpSettingsParcel p) {
        updateViewFromPumpSettingsParcel(p);
        ProgressBar waiting = (ProgressBar) findViewById(R.id.progressBar_getPumpSettingsWaiting);
        waiting.setVisibility(View.INVISIBLE);
        TextView waitingMsg = (TextView) findViewById(R.id.textView_getPumpSettingsProgressMessage);
        waitingMsg.setVisibility(View.INVISIBLE);
    }



    public void updatePumpSettingsView() {
        Log.d(TAG,"Updating pumpSettingsView from parcel");
        String[] msgList = mPumpSettings.getContentsAsStringArray();
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, msgList);
        ListView lv = (ListView) findViewById(R.id.listView_pumpSettings);
        lv.setAdapter(mAdapter);
    }

    public void updateViewFromPumpSettingsParcel(PumpSettingsParcel p) {
        // should use mAdapter.notifyDataSetChanged()?
        mPumpSettings = p;
        // for now, just re-create the view
        updatePumpSettingsView();
    }

    public void getPumpSettingsClicked(View view) {
        Log.w(TAG, "getPumpSettingsClicked");
        Intent intent = new Intent(this, RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.REPORT_PUMP_SETTINGS);
        startService(intent);
        ProgressBar waiting = (ProgressBar) findViewById(R.id.progressBar_getPumpSettingsWaiting);
        waiting.setVisibility(View.VISIBLE);
        TextView waitingMsg = (TextView) findViewById(R.id.textView_getPumpSettingsProgressMessage);
        waitingMsg.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pump_settings, menu);
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intents.ROUNDTRIP_STATUS_MESSAGE);
        intentFilter.addAction(Intents.ROUNDTRIP_TASK_RESPONSE);

        // register our desire to receive broadcasts from RTDemoService
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver, intentFilter);

        // get serial number from preferences
        String sn = updateSerialNumberFromPreferences();
        // On first run, we don't have mPumpSettings yet, so
        // this will end up with default values.  OK?
        updatePumpSettingsView();
    }

    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);
    }

}
