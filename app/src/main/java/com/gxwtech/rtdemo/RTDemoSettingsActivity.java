package com.gxwtech.rtdemo;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/*

This class exists to provide a menu, because the Android Settings Activity system
is too complex for me, currently.  i.e. a hack.
 */


public class RTDemoSettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtdemo_settings);
    }

    public void launchPersonalProfileActivity(View view) {
        Intent intent = new Intent(this,PersonalProfileActivity.class);
        startActivity(intent);
    }

    public void launchPumpSettingsActivity(View view) {
        Intent intent = new Intent(this,PumpSettingsActivity.class);
        startActivity(intent);
    }

    public void launchMongoDBSettingsActivity(View view) {
        Intent intent = new Intent(this,MongoDBSettingsActivity.class);
        startActivity(intent);
    }

    public void launchUtilityActivity(View view) {
        Intent intent = new Intent(this,UtilityActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rtdemo_settings, menu);
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
