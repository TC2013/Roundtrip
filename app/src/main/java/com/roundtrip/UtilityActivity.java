package com.roundtrip;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.roundtrip.roundtrip.R;
import com.roundtrip.services.RTDemoService;

public class UtilityActivity extends ActionBarActivity {
    private static final String TAG = "UtilityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utility);
    }

    public void getHistoryButtonClicked(View view) {
        Log.d(TAG, "GetHistoryButtonClicked");
        Intent intent = new Intent(this, RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.REPORT_PUMP_HISTORY);
        startService(intent);
    }

    public void launchTempBasalsActivity(View view) {
        Intent intent = new Intent(this, TempBasalActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_utility, menu);
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
