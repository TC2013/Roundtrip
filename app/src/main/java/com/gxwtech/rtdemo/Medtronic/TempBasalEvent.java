package com.gxwtech.rtdemo.Medtronic;

import com.gxwtech.rtdemo.Medtronic.PumpData.TempBasalPair;

import org.joda.time.DateTime;

/**
 * Created by geoff on 6/8/15.
 *
 * In the pump's history, temp basals are recorded as 1) a TempBasalRate event,
 * with a timestamp and a rate, and 2) as a separate TempBasalDuration event, with a timestamp
 * and a duration.  This is inconvenient for the rest of the software, so this class puts the two
 * together as a timestamp, duration, and rate in one package.
 *
 */
public class TempBasalEvent {
    public DateTime mTimestamp;
    public TempBasalPair mBasalPair;
    public TempBasalEvent() {
        mTimestamp = new DateTime(0);
        mBasalPair = new TempBasalPair();
    }
    public TempBasalEvent(DateTime timestamp, TempBasalPair pair) {
        mTimestamp = timestamp;
        mBasalPair = pair;
    }
}
