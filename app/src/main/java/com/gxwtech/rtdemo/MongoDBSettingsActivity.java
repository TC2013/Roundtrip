package com.gxwtech.rtdemo;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class MongoDBSettingsActivity extends ActionBarActivity {
    private static final String TAG = "MongoDBSettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mongo_dbsettings);
    }

     // this is run when the SET button is clicked.
    public void saveMongoDBPrefs(View view) {
        EditText editText;
        // gather values from fields
        editText = (EditText)findViewById(R.id.editText_Server);
        String server = editText.getText().toString();
        editText = (EditText)findViewById(R.id.editText_ServerPort);
        String serverPort = editText.getText().toString();
        editText = (EditText) findViewById(R.id.editText_DatabaseName);
        String dbname = editText.getText().toString();
        editText = (EditText)findViewById(R.id.editText_MongoUsername);
        String mongoUsername = editText.getText().toString();
        editText = (EditText)findViewById(R.id.editText_MongoPassword);
        String mongoPassword = editText.getText().toString();
        editText = (EditText)findViewById(R.id.editText_MongoCollectionName);
        String mongoCollection = editText.getText().toString();

        // open prefs for editing
        SharedPreferences preferences = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, MODE_PRIVATE);
        SharedPreferences.Editor edit= preferences.edit();
        // write prefs
        edit.putString(Constants.PrefName.MongoDBServerPrefName, server);
        edit.putString(Constants.PrefName.MongoDBServerPortPrefName,serverPort);
        edit.putString(Constants.PrefName.MongoDBDatabasePrefName,dbname);
        edit.putString(Constants.PrefName.MongoDBUsernamePrefName,mongoUsername);
        edit.putString(Constants.PrefName.MongoDBPasswordPrefName, mongoPassword);
        edit.putString(Constants.PrefName.MongoDBCollectionPrefName, mongoCollection);
        // save prefs
        edit.commit();
        // notify user that the settings were saved
        TextView settingsSavedMsg = (TextView)findViewById(R.id.textView_SaveStatusMsg);
        settingsSavedMsg.setVisibility(View.VISIBLE);
    }

    // update all values from the preference
    public void updateFromPreferences() {
        // open prefs
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
        // get strings from prefs
        String server = settings.getString(Constants.PrefName.MongoDBServerPrefName, "localhost");
        String serverPort = settings.getString(Constants.PrefName.MongoDBServerPortPrefName, "12345");
        String dbname = settings.getString(Constants.PrefName.MongoDBDatabasePrefName, "db");
        String mongoUsername = settings.getString(Constants.PrefName.MongoDBUsernamePrefName, "username");
        String mongoPassword = settings.getString(Constants.PrefName.MongoDBPasswordPrefName, "password");
        String mongoCollection = settings.getString(Constants.PrefName.MongoDBCollectionPrefName, "entries");

        // fill out fields from strings
        EditText editText;
        editText = (EditText)findViewById(R.id.editText_Server);
        editText.setText(server);
        editText = (EditText)findViewById(R.id.editText_ServerPort);
        editText.setText(serverPort);
        editText = (EditText) findViewById(R.id.editText_DatabaseName);
        editText.setText(dbname);
        editText = (EditText)findViewById(R.id.editText_MongoUsername);
        editText.setText(mongoUsername);
        editText = (EditText)findViewById(R.id.editText_MongoPassword);
        editText.setText(mongoPassword);
        editText = (EditText)findViewById(R.id.editText_MongoCollectionName);
        editText.setText(mongoCollection);
        // clear "saved" message
        TextView settingsSavedMsg = (TextView)findViewById(R.id.textView_SaveStatusMsg);
        settingsSavedMsg.setVisibility(View.INVISIBLE);
    }

    protected void onResume() {
        super.onResume();
        updateFromPreferences();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mongo_dbsettings, menu);
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
