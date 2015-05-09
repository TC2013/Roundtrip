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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.gxwtech.rtdemo.Services.PumpManager.PumpSettingsParcel;
import com.gxwtech.rtdemo.Services.RTDemoService;


public class PumpSettingsActivity extends ActionBarActivity {
    private final static String TAG = "PumpSettingsActivity";
    BroadcastReceiver mBroadcastReceiver;
    PumpSettingsParcel mPumpSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPumpSettings = new PumpSettingsParcel();
        setContentView(R.layout.activity_pump_settings);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == Intents.ROUNDTRIP_TASK_RESPONSE) {
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
                }
            }
        };

    }

    public void updateViewFromPumpSettingsParcel(PumpSettingsParcel p) {
        Log.d(TAG,"updateViewFromPumpSettingsParcel");
        EditText autoOffDuration = (EditText)findViewById(R.id.editText_autoOffDuration);
        autoOffDuration.setText(String.format("%d",p.mAutoOffDuration_hours));

        EditText alarmMode = (EditText)findViewById(R.id.editText_alarmMode);
        alarmMode.setText(String.format("%d",p.mAlarmMode));

        EditText alarmVolume = (EditText)findViewById(R.id.editText_alarmVolume);
        alarmVolume.setText(String.format("%d", p.mAlarmVolume));

        Switch audioBolusEnable = (Switch)findViewById(R.id.switch1_audioBolusEnable);
        audioBolusEnable.setChecked(p.mAudioBolusEnable);

        EditText audioBolusSize = (EditText)findViewById(R.id.editText_audioBolusSize);
        audioBolusSize.setText(String.format("%0.2f", p.mAudioBolusSize));

        ToggleButton variableBolusEnable = (ToggleButton)findViewById(R.id.toggleButton_variableBolusEnable);
        variableBolusEnable.setChecked(p.mVariableBolusEnable);

        EditText maxBolus = (EditText)findViewById(R.id.editText_maxBolus);
        maxBolus.setText(String.format("%0.2f",p.mMaxBolus));

        EditText maxBasal = (EditText)findViewById(R.id.editText_maxBasal);
        maxBasal.setText(String.format("%d",p.mMaxBasal));

        EditText timeFormat = (EditText)findViewById(R.id.editText_timeFormat);
        timeFormat.setText(String.format("%d",p.mTimeFormat));

        RadioGroup insulinConcentration = (RadioGroup) findViewById(R.id.radioGroup_insulinConcentration);
        if (p.mInsulinConcentration != 0) {
            insulinConcentration.check(R.id.radioButton_insulinConcentration100);
        } else {
            insulinConcentration.check(R.id.radioButton_insulinConcentration50);
        }

        CheckBox patternsEnable = (CheckBox)findViewById(R.id.checkBox_patternsEnable);
        patternsEnable.setChecked(p.mPatternsEnabled);

        EditText selectedPattern = (EditText)findViewById(R.id.editText_selectedPattern);
        selectedPattern.setText(String.format("%d", p.mSelectedPattern));

        EditText rfEnable = (EditText)findViewById(R.id.editText_RFEnable);
        rfEnable.setText(String.format("%d", p.mRFEnable));

        EditText blockEnable = (EditText)findViewById(R.id.editText_blockEnable);
        blockEnable.setText(String.format("%d", p.mBlockEnable));

        EditText tempBasalType = (EditText)findViewById(R.id.editText_tempBasalType);
        tempBasalType.setText(String.format("%d", p.mTempBasalType));

        EditText tempBasalRate = (EditText)findViewById(R.id.editText_tempBasalRate);
        tempBasalRate.setText(String.format("%d", p.mTempBasalRate));

        EditText paradigmEnable = (EditText)findViewById(R.id.editText_paradigmEnable);
        paradigmEnable.setText(String.format("%d", p.mParadigmEnable));

        EditText insulinActionType = (EditText)findViewById(R.id.editText_insulinActionType);
        insulinActionType.setText(String.format("%d", p.mInsulinActionType));

        EditText lowReservoirWarnType = (EditText)findViewById(R.id.editText_lowReservoirWarnType);
        lowReservoirWarnType.setText(String.format("%d", p.mLowReservoirWarnType));

        EditText lowReservoirWarnPoint = (EditText)findViewById(R.id.editText_lowReservoirWarnPoint);
        lowReservoirWarnPoint.setText(String.format("%d", p.mLowReservoirWarnPoint));

        EditText keypadLockStatus = (EditText)findViewById(R.id.editText_keypadLockStatus);
        keypadLockStatus.setText(String.format("%d", p.mKeypadLockStatus));

    }

    public void getPumpSettingsClicked(View view) {
        Log.w(TAG, "getPumpSettingsClicked");
        Intent intent = new Intent(this, RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.REPORT_PUMP_SETTINGS);
        startService(intent);
        ProgressBar waiting = (ProgressBar) findViewById(R.id.progressBar_getPumpSettingsWaiting);
        waiting.setVisibility(View.VISIBLE);
    }

    public void receivePumpSettingsParcel(PumpSettingsParcel p) {
        Log.w(TAG,"receivePumpSettingsParcel");
        updateViewFromPumpSettingsParcel(p);
        ProgressBar waiting = (ProgressBar) findViewById(R.id.progressBar_getPumpSettingsWaiting);
        waiting.setVisibility(View.INVISIBLE);
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
    }

    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);
    }

}
