package com.gxwtech.rtdemo;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * Created by geoff on 5/29/15.
 */
public class BGReading {
    public DateTime mTimestamp;
    public double mBg;
    public BGReading() { init(new DateTime(0),0); }
    // copy constructor
    public BGReading(BGReading bgr) {
        mTimestamp = bgr.mTimestamp;
        mBg = bgr.mBg;
    }
    public BGReading(DateTime dt, double bg) {
        init(dt,bg);
    }
    public void init(DateTime dt, double bg) {
        mTimestamp = dt;
        mBg = bg;
    }
    public boolean isOlderThan(int minutes) {
        return isOlderThan(minutes,DateTime.now());
    }

    // this function allows a cached "now" to be used.
    public boolean isOlderThan(int minutes, DateTime now) {
        Duration duration = new Duration(mTimestamp,now);
        // todo: test this.
        boolean rval = (duration.getStandardMinutes() > minutes);
        return rval;
    }
}
