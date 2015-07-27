package com.gxwtech.rtdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.gxwtech.rtdemo.services.DIATable;

import java.util.ArrayList;
import java.util.List;


public class LoggingSettingsActivity extends ActionBarActivity {
    private static final String TAG = "LoggingSettingsActivity";
    PreferenceBackedStorage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging_settings);
        mStorage = new PreferenceBackedStorage(getApplicationContext());
    }

    protected void onResume() {
        super.onResume();
        updateFromPreferences();
    }

    public void setLoggingEnableClicked(View view) {
        ToggleButton sw = (ToggleButton)findViewById(R.id.toggleButton_loggingEnable);
        boolean newSetting = sw.isChecked();
        mStorage.loggingEnabled.set(newSetting);
    }

    // get from preferences, load it into proper field
    public void updateFromPreferences() {
        boolean loggingEnabled = mStorage.loggingEnabled.get();
        ToggleButton tb = (ToggleButton)findViewById(R.id.toggleButton_loggingEnable);
        if (tb!=null) {
            tb.setChecked(loggingEnabled);
        } else {
            Log.e(TAG,"toggle button is null(?)");
        }
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
