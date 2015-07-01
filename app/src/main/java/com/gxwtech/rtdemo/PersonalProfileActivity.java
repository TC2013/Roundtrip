package com.gxwtech.rtdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.gxwtech.rtdemo.services.DIATable;

import java.util.ArrayList;
import java.util.List;


public class PersonalProfileActivity extends ActionBarActivity {
    private static final String TAG = "PersonalProfileActivity";
    PreferenceBackedStorage mStorage;
    // have to hold references for post.
    Spinner mNormDIATableSpinner;
    Spinner mNegDIATableSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_profile);
        mStorage = new PreferenceBackedStorage(getApplicationContext());
        Spinner normalDIATableSpinner = (Spinner)findViewById(R.id.spinner_NormalDIATable);
        Spinner negativeDIATableSpinner = (Spinner)findViewById(R.id.spinner_NegativeDIATable);
        List<DIATable> list = new ArrayList<>();
        list.add(new DIATable(DIATable.DIA_0pt5_hour));
        list.add(new DIATable(DIATable.DIA_1_hour));
        list.add(new DIATable(DIATable.DIA_1pt5_hour));
        list.add(new DIATable(DIATable.DIA_2_hour));
        list.add(new DIATable(DIATable.DIA_2pt5_hour));
        list.add(new DIATable(DIATable.DIA_3_hour));
        list.add(new DIATable(DIATable.DIA_3pt5_hour));
        list.add(new DIATable(DIATable.DIA_4_hour));
        list.add(new DIATable(DIATable.DIA_4pt5_hour));
        list.add(new DIATable(DIATable.DIA_5_hour));
        list.add(new DIATable(DIATable.DIA_5pt5_hour));

        ArrayAdapter<DIATable> adapter1 = new ArrayAdapter<DIATable>(this, android.R.layout.simple_spinner_item,list);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        normalDIATableSpinner.setAdapter(adapter1);
        normalDIATableSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                DIATable t = (DIATable)parent.getItemAtPosition(position);
                mStorage.normalDIATable.set(t.mFlavor);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // duplicated code from above.  Should be combined, but I have no patience for it at the moment.
        ArrayAdapter<DIATable> adapter2 = new ArrayAdapter<DIATable>(this, android.R.layout.simple_spinner_item,list);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        negativeDIATableSpinner.setAdapter(adapter2);
        negativeDIATableSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                DIATable t = (DIATable) parent.getItemAtPosition(position);
                mStorage.negativeInsulinDIATable.set(t.mFlavor);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    protected void onResume() {
        super.onResume();
        mNormDIATableSpinner = (Spinner)findViewById(R.id.spinner_NormalDIATable);
        mNegDIATableSpinner = (Spinner)findViewById(R.id.spinner_NegativeDIATable);
        updateFromPreferences();
    }

    public void setBGMaxClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_BGMax);
        double newBGMax = Double.parseDouble(editText.getText().toString());
        if ((newBGMax > 500) || (newBGMax < 40)) {
            return;
        }
        mStorage.bgMin.set(newBGMax);
    }

    public void setTargetBGClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_TargetBG);
        double newTargetBG = Double.parseDouble(editText.getText().toString());
        if ((newTargetBG > 500) || (newTargetBG < 40)) {
            return;
        }
        mStorage.targetBG.set(newTargetBG);
    }

    public void setBGMinClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_BGMin);
        double newBGMin = Double.parseDouble(editText.getText().toString());
        if ((newBGMin > 500) || (newBGMin< 40)) {
            return;
        }
        mStorage.bgMin.set(newBGMin);
    }

    public void setMaxTmpBasalRateClicked(View view) {
        EditText editText = (EditText)findViewById(R.id.editText_MaxTmpBasalRate);
        double newMaxTmpBasalRate = Double.parseDouble(editText.getText().toString());
        if (newMaxTmpBasalRate < 0) {
            newMaxTmpBasalRate = 0.0;
        }
        mStorage.maxTempBasalRate.set(newMaxTmpBasalRate);
    }

    // this is run when the SET button is clicked.
    public void editCARChanged(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_CAR);
        double newCAR = Double.parseDouble(editText.getText().toString());
        if (newCAR < 0) {
            return;
        }
        mStorage.CAR.set(newCAR);
    }
    public void editCarbDelayClicked(View view) {
        EditText editText = (EditText)findViewById(R.id.editText_CarbDelay);
        int newCarbDelay = Integer.parseInt(editText.getText().toString());
        if ((newCarbDelay < 0)||(newCarbDelay > 200)) {
            newCarbDelay = mStorage.carbDelay.mDefaultValue;
        }
        mStorage.carbDelay.set(newCarbDelay);

    }

    public void editLowBGSuspendClicked(View view) {
        EditText editText = (EditText)findViewById(R.id.editText_LowBGSuspend);
        double newLowBGSuspend = Double.parseDouble(editText.getText().toString());
        if (newLowBGSuspend < 0) {
            newLowBGSuspend = 0.0;
        }
        mStorage.lowGlucoseSuspendPoint.set(newLowBGSuspend);
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
        ((EditText)findViewById(R.id.editText_CAR)).setText(String.format("%.1f", mStorage.CAR.get()));
        ((EditText)findViewById(R.id.editText_CarbDelay)).setText(String.format("%d", mStorage.carbDelay.get()));
        ((EditText)findViewById(R.id.editText_MaxTmpBasalRate)).
                setText(String.format("%.3f", mStorage.maxTempBasalRate.get()));
        ((EditText)findViewById(R.id.editText_BGMin)).setText(String.format("%.1f", mStorage.bgMin.get()));
        ((EditText)findViewById(R.id.editText_TargetBG)).setText(String.format("%.1f", mStorage.targetBG.get()));
        ((EditText)findViewById(R.id.editText_BGMax)).setText(String.format("%.1f", mStorage.bgMax.get()));
        ((EditText)findViewById(R.id.editText_LowBGSuspend))
                .setText(String.format("%.1f", mStorage.lowGlucoseSuspendPoint.get()));
        mNormDIATableSpinner.post(new Runnable() {
            @Override
            public void run() {
                mNormDIATableSpinner.setSelection(mStorage.normalDIATable.get() -1);
            }
        });
        mNegDIATableSpinner.post(new Runnable() {
            @Override
            public void run() {
                mNegDIATableSpinner.setSelection(mStorage.negativeInsulinDIATable.get()-1);
            }
        });

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
