package com.roundtrip;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.roundtrip.roundtrip.R;

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
        ToggleButton sw = (ToggleButton) findViewById(R.id.toggleButton_loggingEnable);
        boolean newSetting = sw.isChecked();
        mStorage.loggingEnabled.set(newSetting);
    }

    public void editKLFHClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_KeepLogsForHours);
        int newHours = Integer.parseInt(editText.getText().toString());
        if ((newHours < 0) || (newHours > 200)) {
            newHours = mStorage.keepLogsForHours.getDefault();
        }
        mStorage.keepLogsForHours.set(newHours);

    }

    // get from preferences, load it into proper field
    public void updateFromPreferences() {
        boolean loggingEnabled = mStorage.loggingEnabled.get();
        ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton_loggingEnable);
        if (tb != null) {
            tb.setChecked(loggingEnabled);
        } else {
            Log.e(TAG, "toggle button is null(?)");
        }
        int keepLogHours = mStorage.keepLogsForHours.get();
        EditText editText = (EditText) findViewById(R.id.editText_KeepLogsForHours);
        editText.setText(String.format("%d", keepLogHours));
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
