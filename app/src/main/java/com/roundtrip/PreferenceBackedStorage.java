package com.roundtrip;

import android.content.Context;
import android.content.SharedPreferences;

import com.roundtrip.persistent.PersistentBoolean;
import com.roundtrip.persistent.PersistentDouble;
import com.roundtrip.persistent.PersistentInt;
import com.roundtrip.services.DIATable;

import org.joda.time.DateTime;

/**
 * <p/>
 * Right now this is a simple pass-through class.  Can be changed to cache values in future.
 * Be sure to instantiate this class with the correct context (use getApplicationContext)
 * so that all instances use the same backing store.
 * <p/>
 * The old method was individually storing all preferences items.
 * I want to move all those into here, so that we can have common get/set for each.
 * <p/>
 * This could be broken into subclasses (extends, implements, whichever) for different areas.
 * <p/>
 * All of these could be changed into extensions of basic types (Int, Double) and set to persist,
 * along with default values and key names
 */
public class PreferenceBackedStorage {
    public PersistentDouble monitorTempBasalRate;
    public PersistentInt monitorTempBasalDuration;
    public PersistentDouble monitorCurrBasalRate;
    public PersistentDouble monitorPredictedBG;
    public PersistentDouble monitorIOB;
    public PersistentDouble monitorCOB;
    public PersistentDouble lowGlucoseSuspendPoint;
    public PersistentDouble CAR;
    public PersistentDouble ISF;
    public PersistentDouble maxTempBasalRate;
    public PersistentDouble bgMin;
    public PersistentDouble targetBG;
    public PersistentDouble bgMax;
    public PersistentInt normalDIATable;
    public PersistentInt negativeInsulinDIATable;
    public PersistentBoolean loggingEnabled;
    public PersistentInt keepLogsForHours;
    Context mContext; // be careful, can leak contexts
    SharedPreferences p;

    public PreferenceBackedStorage(Context ctx) {
        mContext = ctx;
        p = mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);

    /*
     * Low Glucose Suspend Point
     * This is set via the GUI.
     * When APSLogic.MakeADecision() runs, if the latestBGReading is recent (+/- 10 minutes)
     * and latestBGReading is below this number, APSLogic will command pump to temp-basal at zero for 30 minutes.
     * If this value is set to zero, this feature is implicitly disabled.
     *
     * Defaults to 85 mg/dL.
     *
     */

        lowGlucoseSuspendPoint = new PersistentDouble(p, Constants.PrefName.LowGlucoseSuspendPoint, 85.0f);
        CAR = new PersistentDouble(p, Constants.PrefName.CARPrefName, 30.0f);
        ISF = new PersistentDouble(p, Constants.PrefName.ISFPrefName, 40.0f);
        maxTempBasalRate = new PersistentDouble(p, Constants.PrefName.PPMaxTempBasalRatePrefName, 6.1f);
        bgMin = new PersistentDouble(p, Constants.PrefName.PPBGMinPrefName, 95.0f);
        targetBG = new PersistentDouble(p, Constants.PrefName.PPTargetBGPrefName, 115.0f);
        bgMax = new PersistentDouble(p, Constants.PrefName.PPBGMaxPrefName, 125.0f);

        monitorTempBasalRate = new PersistentDouble(p, Constants.PrefName.Monitor_TempBasalRate, -99.0f);
        monitorTempBasalDuration = new PersistentInt(p, Constants.PrefName.Monitor_TempBasalDuration, -99);
        monitorCurrBasalRate = new PersistentDouble(p, Constants.PrefName.Monitor_CurrBasalRate, -99.0f);
        monitorPredictedBG = new PersistentDouble(p, Constants.PrefName.Monitor_PredBG, -99.0f);
        monitorIOB = new PersistentDouble(p, Constants.PrefName.Monitor_IOB, -99.0f);
        monitorCOB = new PersistentDouble(p, Constants.PrefName.Monitor_COB, -99.0f);

        normalDIATable = new PersistentInt(p, Constants.PrefName.PPNormalDIATable, DIATable.DIA_3_hour);
        negativeInsulinDIATable = new PersistentInt(p, Constants.PrefName.PPNegativeInsulinDIATable, DIATable.DIA_2_hour);
        loggingEnabled = new PersistentBoolean(p, Constants.PrefName.LoggingEnabled, false);
        keepLogsForHours = new PersistentInt(p, Constants.PrefName.KeepLogsForHours, 24);


    }

    /*
     *
     * Latest BG Reading
     *
     * This is updated from RTDemoService at the start of each run of APSLogic.
     * This needs to be converted to using the Persistent<type> functions.
     *
     */
    public BGReading getLatestBGReading() {
        final String bad_ts_value = "(never)";
        final float bad_bg_value = -10000.1f;
        BGReading rval = new BGReading();

        String ts = bad_ts_value;
        try {
            ts = p.getString(Constants.PrefName.LatestBGTimestamp, bad_ts_value);
        } catch (ClassCastException e) {
            return rval;
        }
        if (ts.equals(bad_ts_value)) {
            return rval;
        }
        DateTime timestamp = DateTime.parse(ts);

        float bgr = p.getFloat(Constants.PrefName.LatestBGReading, bad_bg_value);
        if (bgr == bad_bg_value) {
            return rval;
        }
        rval = new BGReading(timestamp, bgr);
        return rval;
    }

    public void setLatestBGReading(BGReading r) {
        // cram it in the preferences, with everything else.
        if (mContext == null) {
            return;
        }
        SharedPreferences.Editor edit = p.edit();
        edit.putString(Constants.PrefName.LatestBGTimestamp, r.mTimestamp.toString());
        edit.putFloat(Constants.PrefName.LatestBGReading, (float) r.mBg);
        edit.commit();
    }


}
