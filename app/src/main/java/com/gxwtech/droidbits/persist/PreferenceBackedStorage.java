package com.gxwtech.droidbits.persist;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by geoff on 6/23/15.
 */
public class PreferenceBackedStorage {
    Context mContext; // be careful, can leak contexts
    SharedPreferences p;
    public PreferenceBackedStorage(Context ctx,String preferenceStorageName) {
        mContext = ctx;
        p = mContext.getSharedPreferences(preferenceStorageName,0);
    }
}
