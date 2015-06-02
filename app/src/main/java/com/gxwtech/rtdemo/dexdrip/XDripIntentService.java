package com.gxwtech.rtdemo.dexdrip;

import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.gxwtech.rtdemo.BGReading;
import com.gxwtech.rtdemo.Services.RTDemoService;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Used by XDripDataReceiver to store new estimates from DexDrip.
 *
 * @see XDripDataReceiver
 *
 * Cribbed from Stephen Black's Nightwatch project
 * GGW: unfortunately, our BG classes are different, so I've munged this to use Roundtrip's simplified BG.
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
                BGReading bg = new BGReading();
                Long ts = intent.getLongExtra(XDripIntents.EXTRA_TIMESTAMP, new Date().getTime());
                // FIXME: how to handle time zones with joda DateTime?
                DateTime dt = new DateTime(ts);

                bg.mTimestamp = dt;
                bg.mBg = intent.getDoubleExtra(XDripIntents.EXTRA_BG_ESTIMATE, 0);

                // TODO: Broadcast the new BG reading locally (ie.e. this app only)
                RTDemoService.getInstance().receiveXDripBGEstimate(bg);
            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
