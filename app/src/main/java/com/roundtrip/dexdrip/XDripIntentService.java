package com.roundtrip.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.roundtrip.Constants;

import java.util.Date;

/**
 * Used by XDripDataReceiver to store new estimates from DexDrip.
 *
 * @see XDripDataReceiver
 * <p/>
 * Cribbed from Stephen Black's Nightwatch project
 * GGW: unfortunately, our BG (blood glucose) classes are different, so I've munged this to use Roundtrip's simplified BG.
 */
public class XDripIntentService extends android.app.IntentService {
    public static final String ACTION_NEW_DATA = "com.dexdrip.stephenblack.nightwatch.action.NEW_DATA";

    public XDripIntentService() {
        super("DexDripIntentService");
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        final String action = intent.getAction();

        try {
            if (ACTION_NEW_DATA.equals(action)) {
                Long ts = intent.getLongExtra(XDripIntents.EXTRA_TIMESTAMP, new Date().getTime());
                double bg = intent.getDoubleExtra(XDripIntents.EXTRA_BG_ESTIMATE, 0);

                // cram it in the preferences, with everything else.
                SharedPreferences settings = getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0);
                SharedPreferences.Editor edit = settings.edit();
                edit.putLong(Constants.PrefName.LatestBGTimestamp, ts);
                edit.putFloat(Constants.PrefName.LatestBGReading, (float) bg);
                edit.commit();
            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
