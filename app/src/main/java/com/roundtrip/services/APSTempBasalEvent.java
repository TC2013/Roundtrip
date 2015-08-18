package com.roundtrip.services;

import com.roundtrip.medtronic.TempBasalEvent;

import org.joda.time.DateTime;

/**
 * <p/>
 * This class holds the extra data we need, above what the Minimed temp basal event holds.
 */
public class APSTempBasalEvent extends TempBasalEvent {
    // actual duration is from start until end, or now, if still active.
    public int actualDurationMinutes = 0;
    // Note that endtime may be after the current time, if the temp basal is still active.
    public DateTime endtime = new DateTime(0);
    public double mTotalRelativeInsulin = 0.0; // total relative insulin delivered by this event.

    public APSTempBasalEvent() {
    }

    public APSTempBasalEvent(TempBasalEvent ev) {
        mTimestamp = ev.mTimestamp;
        mBasalPair.mDurationMinutes = ev.mBasalPair.mDurationMinutes;
        mBasalPair.mInsulinRate = ev.mBasalPair.mInsulinRate;
    }

    public boolean isRecent() {
        // isRecent is defined by being within the maximum insulin effect time plus 30 minutes.
        return (endtime.isAfter(DateTime.now().minusMinutes(DIATable.insulinImpactMinutesMax + 30)));
    }

}
