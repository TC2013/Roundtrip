package com.gxwtech.droidbits.persist;

import android.content.SharedPreferences;
import org.joda.time.DateTime;

/**
 * Created by geoff on 6/29/15.
 * This is a base class for the double and int persistant values.  Should probably be an interface.
 */
public class PersistentValue {
    protected String mName;
    protected static final String ts_suffix = "_PVTimestamp";
    SharedPreferences mp;
    public PersistentValue(SharedPreferences p, String name) {
        mName = name;
        mp = p;
    }

    public DateTime getTimestamp() {
        return DateTime.parse(mp.getString(mName + ts_suffix,""));
    }

}
