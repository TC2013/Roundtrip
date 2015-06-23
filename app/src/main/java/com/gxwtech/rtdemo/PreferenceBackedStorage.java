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

}
