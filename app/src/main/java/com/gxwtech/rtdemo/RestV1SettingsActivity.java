package com.gxwtech.rtdemo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;


public class RestV1SettingsActivity extends ActionBarActivity {
    private static final String TAG = "RestV1SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest_v1_settings);
    }

     // this is run when the SET button is clicked.
    public void savePrefs(View view) {
        EditText editText;
        // gather values from fields
        editText = (EditText)findViewById(R.id.editText_URI);
        String restURI = editText.getText().toString();
        CheckBox cb = (CheckBox)findViewById(R.id.checkBox_allowWritingToDB);
        boolean allowWritingtoDB = cb.isChecked();
        URI testURI = null;
        try {
            testURI = new URI(restURI);
        } catch (URISyntaxException e) {
            testURI = null;
        }

        if (testURI != null) {
            // open prefs for editing
            SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
            SharedPreferences.Editor edit= preferences.edit();
            // write prefs
            edit.putString(Constants.PrefName.RestURI,restURI);
            edit.putBoolean(Constants.PrefName.RestAllowWrite, allowWritingtoDB);

            // save prefs
            edit.commit();
            // notify user that the settings were saved
            TextView settingsSavedMsg = (TextView)findViewById(R.id.textView_SaveStatusMsg);
            settingsSavedMsg.setVisibility(View.VISIBLE);

        } else {
            Toast.makeText(this, "Syntax Error in URI", Toast.LENGTH_LONG).show();
        }
    }

    // update all values from the preference
    public void updateFromPreferences() {
        // open prefs
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        // get strings from prefs

        String restURI = settings.getString(Constants.PrefName.RestURI,Constants.defaultRestURI);
        boolean allowWritingToDB = settings.getBoolean(Constants.PrefName.RestAllowWrite, true);

        // fill out fields from strings
        EditText editText;
        editText = (EditText)findViewById(R.id.editText_URI);
        editText.setText(restURI);
        CheckBox cb = (CheckBox)findViewById(R.id.checkBox_allowWritingToDB);
        cb.setChecked(allowWritingToDB);
        // clear "saved" message
        TextView settingsSavedMsg = (TextView)findViewById(R.id.textView_SaveStatusMsg);
        settingsSavedMsg.setVisibility(View.INVISIBLE);
    }

    protected void onResume() {
        super.onResume();
        updateFromPreferences();
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
