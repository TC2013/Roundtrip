package com.roundtrip;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.roundtrip.medtronic.PumpData.TempBasalPair;
import com.roundtrip.roundtrip.R;
import com.roundtrip.services.RTDemoService;
import com.roundtrip.services.pumpmanager.TempBasalPairParcel;

public class TempBasalActivity extends ActionBarActivity {
    private static final String TAG = "TempBasalActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_basal);
    }

    public void tempBasalZero30Clicked(View view) {
        Log.d(TAG, "tempBasalZero30Clicked");
        SendSetTempBasalRequest(new TempBasalPair(0.0, 30));
    }

    public void CancelTempBasalClicked(View view) {
        Log.d(TAG, "cancelTempBasalClicked");
        SendSetTempBasalRequest(new TempBasalPair(0.0, 0));
    }

    protected void SendSetTempBasalRequest(TempBasalPair pair) {
        Intent intent = new Intent(this, RTDemoService.class);
        intent.putExtra("srq", Constants.SRQ.SET_TEMP_BASAL);
        intent.putExtra("name", Constants.ParcelName.TempBasalPairParcelName);
        intent.putExtra(Constants.ParcelName.TempBasalPairParcelName, new TempBasalPairParcel(pair));
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_temp_basal, menu);
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
