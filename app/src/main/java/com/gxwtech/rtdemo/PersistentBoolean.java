package com.gxwtech.rtdemo;

import android.content.SharedPreferences;

import org.joda.time.DateTime;

/**
 * Created by geoff on 7/1/15.
 */
public class PersistentBoolean extends PersistentValue {
    boolean mDefaultValue;
    public PersistentBoolean(SharedPreferences p, String name, boolean defaultValue) {
        super(p,name);
        mDefaultValue = defaultValue;
    }
    public boolean get() {
        return mp.getBoolean(mName, mDefaultValue);
    }
    public void set(boolean newvalue) {
        mp.edit().putBoolean(mName, newvalue)
                .putString(mName + ts_suffix, DateTime.now().toDateTimeISO().toString())
                .commit();
    }

}
