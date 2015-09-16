package com.gxwtech.droidbits.persist;

import android.content.SharedPreferences;

import org.joda.time.DateTime;

/**
 * Created by geoff on 6/29/15.
 *
 * Note: although this class is named PersistentDouble, it persists as a float.
 */
public class PersistentDouble extends PersistentValue {
    double mDefaultValue;
    public PersistentDouble(SharedPreferences p, String name, double defaultValue) {
        super(p,name);
        mDefaultValue = defaultValue;
    }
    public double get() {
        return mp.getFloat(mName,(float)mDefaultValue);
    }
    public void set(double newvalue) {
        mp.edit().putFloat(mName, (float) newvalue)
                .putString(mName + ts_suffix, DateTime.now().toDateTimeISO().toString())
                .commit();
    }
}
