package com.gxwtech.rtdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gxwtech.rtdemo.Services.RTDemoService;


public class PersonalProfileActivity extends ActionBarActivity {
    private static final String TAG = "PersonalProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_profile);
        updateISFFromPreferences();
        updateCARFromPreferences();
    }

    // this is run when the SET button is clicked.
    public void editISFChanged(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_ISF);
        double newISF = Double.parseDouble(editText.getText().toString());
        Log.w(TAG, String.format("editISFChanged: %.1f",newISF));
        // save in SharedPreferences
        SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
        SharedPreferences.Editor edit= preferences.edit();
        edit.putFloat(Constants.PrefName.ISFPrefName, (float) newISF);
        edit.commit();
        setISF(newISF);
    }

    // Announce to the rest of the app that the ISF has changed.
    public void setISF(double isf) {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.SET_ISF);
        intent.putExtra("isf", isf);
        startService(intent);
    }

    // get ISF from preferences, load it into proper field
    public String updateISFFromPreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        double isf = (double)settings.getFloat(Constants.PrefName.ISFPrefName, (float) 45.0);
        EditText editText = (EditText)findViewById(R.id.editText_ISF);
        String isfString = String.format("%.1f", isf);
        editText.setText(isfString);
        return isfString;
    }

    // this is run when the SET button is clicked.
    public void editCARChanged(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_CAR);
        double newCAR = Double.parseDouble(editText.getText().toString());
        Log.w(TAG, String.format("editCARChanged: %.1f",newCAR));
        // save in SharedPreferences
        SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
        SharedPreferences.Editor edit= preferences.edit();
        edit.putFloat(Constants.PrefName.CARPrefName, (float) newCAR);
        edit.commit();
        setCAR(newCAR);
    }

    // Announce to the rest of the app that the CAR has changed.
    public void setCAR(double car) {
        Intent intent = new Intent(this,RTDemoService.class);
        intent.putExtra("what", Constants.SRQ.SET_CAR);
        intent.putExtra("car", car);
        startService(intent);
    }

    // get ISF from preferences, load it into proper field
    public String updateCARFromPreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        double car = (double)settings.getFloat(Constants.PrefName.CARPrefName, (float) 30.0);
        EditText editText = (EditText)findViewById(R.id.editText_ISF);
        String carString = String.format("%.1f",car);
        editText.setText(carString);
        return carString;
    }

    @Override
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
