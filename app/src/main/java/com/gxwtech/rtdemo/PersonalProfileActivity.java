package com.gxwtech.rtdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gxwtech.rtdemo.Services.RTDemoService;


public class PersonalProfileActivity extends ActionBarActivity {
    private static final String TAG = "PersonalProfileActivity";
    double mCAR;
    double mBGMax;
    double mTargetBG;
    double mBGMin;
    double mMaxTmpBasalRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_profile);
    }

    protected void onResume() {
        super.onResume();
        updateFromPreferences();
    }

    public void setBGMaxClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_BGMax);
        double newBGMax = Double.parseDouble(editText.getText().toString());
        if ((newBGMax > 500) || (newBGMax < 40)) {
            return;
        }
        mBGMax = newBGMax;
        savePreferences();
    }

    public void setTargetBGClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_TargetBG);
        double newTargetBG = Double.parseDouble(editText.getText().toString());
        if ((newTargetBG > 500) || (newTargetBG < 40)) {
            return;
        }
        mTargetBG = newTargetBG;
        savePreferences();
    }

    public void setBGMinClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_BGMin);
        double newBGMin = Double.parseDouble(editText.getText().toString());
        if ((newBGMin > 500) || (newBGMin< 40)) {
            return;
        }
        mBGMin = newBGMin;
        savePreferences();
    }

    public void setMaxTmpBasalRateClicked(View view) {
        EditText editText = (EditText)findViewById(R.id.editText_MaxTmpBasalRate);
        double newMaxTmpBasalRate = Double.parseDouble(editText.getText().toString());
        if (newMaxTmpBasalRate < 0) {
            newMaxTmpBasalRate = 0.0;
        }
        mMaxTmpBasalRate = newMaxTmpBasalRate;
        savePreferences();
    }

    // this is run when the SET button is clicked.
    public void editCARChanged(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_CAR);
        double newCAR = Double.parseDouble(editText.getText().toString());
        if (newCAR < 0) {
            return;
        }
        mCAR = newCAR;
        savePreferences();
    }

    public void savePreferences() {
        // save in SharedPreferences
        SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putFloat(Constants.PrefName.CARPrefName, (float) mCAR);
        edit.putFloat(Constants.PrefName.PPMaxTempBasalRatePrefName, (float) mMaxTmpBasalRate);
        edit.putFloat(Constants.PrefName.PPBGMinPrefName, (float) mBGMin);
        edit.putFloat(Constants.PrefName.PPTargetBGPrefName,(float)mTargetBG);
        edit.putFloat(Constants.PrefName.PPBGMaxPrefName, (float) mBGMax);
        edit.commit();
        announcePreferenceChanges();
    }

    public void announcePreferenceChanges() {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.PERSONAL_PREFERENCE_CHANGE);
        startService(intent);
    }

    // get from preferences, load it into proper field
    public void updateFromPreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);

        mCAR = (double)settings.getFloat(Constants.PrefName.CARPrefName, (float) 30.0);
        EditText editText = (EditText)findViewById(R.id.editText_CAR);
        String str = String.format("%.1f",mCAR);
        editText.setText(str);

        mMaxTmpBasalRate = (double)settings.getFloat(Constants.PrefName.PPMaxTempBasalRatePrefName,(float)6.1);
        editText = (EditText)findViewById(R.id.editText_MaxTmpBasalRate);
        str = String.format("%.3f", mMaxTmpBasalRate);
        editText.setText(str);

        mBGMin = (double)settings.getFloat(Constants.PrefName.PPBGMinPrefName, (float) 95.0);
        editText = (EditText)findViewById(R.id.editText_BGMin);
        str = String.format("%.1f", mBGMin);
        editText.setText(str);

        mTargetBG = (double)settings.getFloat(Constants.PrefName.PPTargetBGPrefName, (float) 115.0);
        editText = (EditText)findViewById(R.id.editText_TargetBG);
        str = String.format("%.1f", mTargetBG);
        editText.setText(str);

        mBGMax = (double)settings.getFloat(Constants.PrefName.PPBGMaxPrefName, (float) 125.0);
        editText = (EditText)findViewById(R.id.editText_BGMax);
        str = String.format("%.1f",mBGMax);
        editText.setText(str);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_personal_profile, menu);
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
