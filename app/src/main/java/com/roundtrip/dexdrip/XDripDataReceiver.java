package com.roundtrip.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Tells us when G4 data has been updated.
 * Cribbed from Stephen Black's Nightwatch project
 */
public class XDripDataReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, new Intent(context, XDripIntentService.class)
                .setAction(XDripIntentService.ACTION_NEW_DATA)
                .putExtras(intent));
    }
}
