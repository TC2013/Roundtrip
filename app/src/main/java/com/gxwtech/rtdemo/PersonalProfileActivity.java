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


public class PersonalProfileActivity extends ActionBarActivity {
    private static final String TAG = "PersonalProfileActivity";
    PreferenceBackedStorage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_profile);
        mStorage = new PreferenceBackedStorage(getApplicationContext());
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
        mStorage.setBGMax(newBGMax);
    }

    public void setTargetBGClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_TargetBG);
        double newTargetBG = Double.parseDouble(editText.getText().toString());
        if ((newTargetBG > 500) || (newTargetBG < 40)) {
            return;
        }
        mStorage.setTargetBG(newTargetBG);
    }

    public void setBGMinClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_BGMin);
        double newBGMin = Double.parseDouble(editText.getText().toString());
        if ((newBGMin > 500) || (newBGMin< 40)) {
            return;
        }
        mStorage.setBGMin(newBGMin);
    }

    public void setMaxTmpBasalRateClicked(View view) {
        EditText editText = (EditText)findViewById(R.id.editText_MaxTmpBasalRate);
        double newMaxTmpBasalRate = Double.parseDouble(editText.getText().toString());
        if (newMaxTmpBasalRate < 0) {
            newMaxTmpBasalRate = 0.0;
        }
        mStorage.setMaxTempBasalRate(newMaxTmpBasalRate);
    }

    // this is run when the SET button is clicked.
    public void editCARChanged(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_CAR);
        double newCAR = Double.parseDouble(editText.getText().toString());
        if (newCAR < 0) {
            return;
        }
        mStorage.setCAR(newCAR);
    }

    public void editLowBGSuspendClicked(View view) {
        EditText editText = (EditText)findViewById(R.id.editText_LowBGSuspend);
        double newLowBGSuspend = Double.parseDouble(editText.getText().toString());
        if (newLowBGSuspend < 0) {
            newLowBGSuspend = 0.0;
        }
        mStorage.setLowGlucoseSuspendPoint(newLowBGSuspend);
    }

    /*

    If we want to tell the rest of the app that the personal Preferences have changed,
    here's how:

    public void announcePreferenceChanges() {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.PERSONAL_PREFERENCE_CHANGE);
        startService(intent);
    }
    */

    // get from preferences, load it into proper field
    public void updateFromPreferences() {
        ((EditText)findViewById(R.id.editText_CAR)).setText(String.format("%.1f", mStorage.getCAR()));
        ((EditText)findViewById(R.id.editText_MaxTmpBasalRate)).
                setText(String.format("%.3f", mStorage.getMaxTempBasalRate()));
        ((EditText)findViewById(R.id.editText_BGMin)).setText(String.format("%.1f", mStorage.getBGMin()));
        ((EditText)findViewById(R.id.editText_TargetBG)).setText(String.format("%.1f",mStorage.getTargetBG()));
        ((EditText)findViewById(R.id.editText_BGMax)).setText(String.format("%.1f",mStorage.getBGMax()));
        ((EditText)findViewById(R.id.editText_LowBGSuspend))
                .setText(String.format("%.1f",mStorage.getLowGlucoseSuspendPoint()));
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
