package com.gxwtech.rtdemo;

import android.content.Context;
import android.content.SharedPreferences;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by geoff on 6/23/15.
 *
 * Right now this is a simple pass-through class.  Can be changed to cache values in future.
 * Be sure to instantiate this class with the correct context (use getApplicationContext)
 * so that all instances use the same backing store.
 *
 * The old method was individually storing all preferences items.
 * I want to move all those into here, so that we can have common get/set for each.
 *
 * This could be broken into subclasses (extends, implements, whichever) for different areas.
 *
 * All of these could be changed into extensions of basic types (Int, Double) and set to persist,
 * along with default values and key names
 *
 */
public class PreferenceBackedStorage {
    Context mContext; // be careful, can leak contexts
    SharedPreferences p;
    public PreferenceBackedStorage(Context ctx) {
        mContext = ctx;
        p = mContext.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName,0);
    }
    /*
     *
     * Latest BG Reading
     *
     * This is updated from RTDemoService at the start of each run of APSLogic.
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

        float bgr = p.getFloat(Constants.PrefName.LatestBGReading,bad_bg_value);
        if (bgr == bad_bg_value) {
            return rval;
        }
        rval = new BGReading(timestamp,bgr);
        return rval;
    }

    public void setLatestBGReading(BGReading r) {
        // cram it in the preferences, with everything else.
        if (mContext == null) {
            return;
        }
        SharedPreferences.Editor edit = p.edit();
        edit.putString(Constants.PrefName.LatestBGTimestamp,r.mTimestamp.toString());
        edit.putFloat(Constants.PrefName.LatestBGReading,(float)r.mBg);
        edit.commit();
    }

    /*
     * Low Glucose Suspend Point
     * This is set via the GUI.
     * When APSLogic.MakeADecision() runs, if the latestBGReading is recent (+/- 10 minutes)
     * and latestBGReading is below this number, APSLogic will command pump to temp-basal at zero for 30 minutes.
     *
     * Defaults to 85 mg/dL.
     *
     */
    public double getLowGlucoseSuspendPoint() {
        double rval = p.getFloat(Constants.PrefName.LowGlucoseSuspendPoint,85.0f);
        return rval;
    }
    public void setLowGlucoseSuspendPoint(double newPoint) {
        SharedPreferences.Editor edit = p.edit();
        edit.putFloat(Constants.PrefName.LowGlucoseSuspendPoint,(float)newPoint);
        edit.commit();
    }

    public void setCAR(double car) {
        p.edit().putFloat(Constants.PrefName.CARPrefName,(float) car).commit();
    }

    public double getCAR() {
        return p.getFloat(Constants.PrefName.CARPrefName,30.0f);
    }

    public void setMaxTempBasalRate(double rate) {
        p.edit().putFloat(Constants.PrefName.PPMaxTempBasalRatePrefName,(float)rate).commit();
    }
    public double getMaxTempBasalRate() {
        return p.getFloat(Constants.PrefName.PPMaxTempBasalRatePrefName,6.1f);
    }
    public void setBGMin(double bgMin) {
        p.edit().putFloat(Constants.PrefName.PPBGMinPrefName,(float)bgMin).commit();
    }
    public double getBGMin() {
        return p.getFloat(Constants.PrefName.PPBGMinPrefName,95.0f);
    }
    public void setTargetBG(double targetBG) {
        p.edit().putFloat(Constants.PrefName.PPTargetBGPrefName,(float)targetBG).commit();
    }
    public double getTargetBG() {
        return p.getFloat(Constants.PrefName.PPTargetBGPrefName,115.0f);
    }
    public void setBGMax(double bgMax) {
        p.edit().putFloat(Constants.PrefName.PPBGMaxPrefName,(float)bgMax).commit();
    }
    public double getBGMax() {
        return p.getFloat(Constants.PrefName.PPBGMaxPrefName,125.0f);
    }

    // these are specific to the MonitorActivity UI, so could be moved to another (sub) class
    public void setMonitor_TempBasalRate(double rate) {
        p.edit().putFloat(Constants.PrefName.Monitor_TempBasalRate,(float)rate).commit();
    }

    public double getMonitor_TempBasalRate() {
        return p.getFloat(Constants.PrefName.Monitor_TempBasalRate,-99.0f);
    }

    public void setMonitor_TempBasalDuration(int duration_minutes) {
        p.edit().putInt(Constants.PrefName.Monitor_TempBasalDuration,duration_minutes).commit();
    }

    public int getMonitor_TempBasalDuration() {
        return p.getInt(Constants.PrefName.Monitor_TempBasalDuration,-99);
    }

    public void setMonitor_CurrBasalRate(double rate) {
        p.edit().putFloat(Constants.PrefName.Monitor_CurrBasalRate,(float)rate).commit();
    }

    public double getMonitor_CurrBasalRate() {
        return p.getFloat(Constants.PrefName.Monitor_CurrBasalRate,-99.0f);
    }

    public void setMonitor_PredBG(double predBG) {
        p.edit().putFloat(Constants.PrefName.Monitor_PredBG,(float)predBG).commit();
    }

    public double getMonitor_PredBG() {
        return p.getFloat(Constants.PrefName.Monitor_PredBG,-99.0f);
    }

    public void setMonitor_IOB(double iob) {
        p.edit().putFloat(Constants.PrefName.Monitor_IOB,(float)iob).commit();
    }

    public double getMonitor_IOB() {
        return p.getFloat(Constants.PrefName.Monitor_IOB,-99.0f);
    }

    public void setMonitor_COB(double cob) {
        p.edit().putFloat(Constants.PrefName.Monitor_COB,(float)cob).commit();
    }

    public double getMonitor_COB() {
        return p.getFloat(Constants.PrefName.Monitor_COB,-99.0f);
    }
}
