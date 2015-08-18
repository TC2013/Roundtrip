package com.roundtrip.persistent;

import android.content.SharedPreferences;

import org.joda.time.DateTime;

public class PersistentInt extends PersistentValue {
    int mDefaultValue;

    public PersistentInt(SharedPreferences p, String name, int defaultValue) {
        super(p, name);
        mDefaultValue = defaultValue;
    }

    public int get() {
        return mp.getInt(mName, mDefaultValue);
    }

    public int getDefault() {
        return mDefaultValue;
    }

    public void set(int newvalue) {
        mp.edit().putInt(mName, newvalue)
                .putString(mName + ts_suffix, DateTime.now().toDateTimeISO().toString())
                .commit();
    }

}
