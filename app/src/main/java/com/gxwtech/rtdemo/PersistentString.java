package com.gxwtech.rtdemo;

import android.content.SharedPreferences;

import org.joda.time.DateTime;

/**
 * Created by geoff on 6/29/15.
 *
 */
public class PersistentString extends PersistentValue {
    String mDefaultValue;
    public PersistentString(SharedPreferences p, String name, String defaultValue) {
        super(p,name);
        mDefaultValue = defaultValue;
    }
    public String get() {
        return mp.getString(mName,mDefaultValue);
    }
    public void set(String newvalue) {
        mp.edit().putString(mName, newvalue)
                .putString(mName + ts_suffix, DateTime.now().toDateTimeISO().toString())
                .commit();
    }
}
