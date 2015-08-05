package com.gxwtech.rtdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gxwtech.rtdemo.services.RTDemoService;


public class SuspendAPSActivity extends ActionBarActivity {
    int mMinutes; // one and a half hours is stored as 90 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suspend_aps);
    }

    protected void updateSuspendMinutesField(int minutes) {
        EditText editText = (EditText) findViewById(R.id.editText_SuspendMinutes);
        int remainder = minutes % 60;
        String remainderText = String.format("%d", remainder);
        editText.setText(remainderText);
    }

    protected void updateSuspendHoursField(int minutes) {
        EditText editText = (EditText) findViewById(R.id.editText_SuspendHours);
        int hrs = minutes / 60;
        String hrsText = String.format("%d", hrs);
        editText.setText(hrsText);
    }

    protected void getSettingsFromPreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        int minutes = 0;
        try {
            minutes = (int) settings.getInt(Constants.PrefName.SuspendMinutesPrefName, 0);
        } catch (ClassCastException e) {
        }
        mMinutes = minutes;
        updateSuspendMinutesField(minutes);
        updateSuspendHoursField(minutes);
    }

    protected void saveSettingsToPreferences() {
        mMinutes = 0;

        EditText minutesText = (EditText) findViewById(R.id.editText_SuspendMinutes);
        String minutesString = minutesText.getText().toString();
        int minutes = Integer.parseInt(minutesString);
        if (minutes < 0) {
            minutes = 0;
        }
        mMinutes += minutes;

        EditText hoursText = (EditText) findViewById(R.id.editText_SuspendHours);
        String hoursString = hoursText.getText().toString();
        int hours = Integer.parseInt(hoursString);
        if (hours < 0) {
            hours = 0;
        }
        mMinutes += hours * 60;

        // save in SharedPreferences
        SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(Constants.PrefName.SuspendMinutesPrefName, mMinutes);
        edit.commit();
    }

    public void buttonSuspendClicked(View view) {
        saveSettingsToPreferences();
        doSuspend(mMinutes);
    }

    public void buttonCancelClicked(View view) {
        doCancel();
    }

    protected void doSuspend(int minutes) {
        saveSettingsToPreferences();
        // tell the background service to suspend
        Intent intent = new Intent(this, RTDemoService.class);
        // background service will pull the delay amount from the saved preferences.
        intent.putExtra("srq", Constants.SRQ.DO_SUSPEND_MINUTES);
        startService(intent);
        finish();
    }

    protected void doCancel() {
        // end of SuspendAPSActivity
        finish();
    }

    protected void onResume() {
        super.onResume();
        getSettingsFromPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_suspend_aps, menu);
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
