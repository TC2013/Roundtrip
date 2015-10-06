package com.gxwtech.rtdemo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by geoff on 6/30/15.
 */
public class DBTempBasalEntry {
    public static String enteredByString = "enteredBy";
    public static String enteredBy = "Roundtrip";
    public static String eventTypeString = "eventType";
    public static String eventType = "Temp Basal";
    public double mRelativeInsulin = 0.0;  // total insulin delivered, in units
    public int mDurationMinutes = 0; // actual duration of delivery
    public DateTime mTimestamp = new DateTime(0);
    public String startTime = "(none)";
    public String endTime = "(none)";

    public DBTempBasalEntry() {
    }

    public DBTempBasalEntry(DateTime timestamp, double relativeInsulin, int actualDurationMinutes) {
        mTimestamp = timestamp;
        mRelativeInsulin = relativeInsulin;
        mDurationMinutes = actualDurationMinutes;
        calcStartEndTimes();
    }

    protected void calcStartEndTimes() {
        // see http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
        String formatString = "YY/MM/dd hh:mmaa";
        startTime = mTimestamp.toString(formatString);
        endTime = mTimestamp.plusMinutes(mDurationMinutes).toString(formatString);
    }

}


/*

Example treatment event, as it appears in the MongoDB
when entered from the nightscout gui

    {
    "_id" : {
        "$oid": "55917b1b4f4a0920112cd765"
    },
    "enteredBy": "",
    "eventType": "Carb Correction",
    "insulin": "1.5",
    "notes": "Start: 10:03am\nEnd: 10:33am",
    "created_at": "2015-06-29T17:06:35.879Z"
    }
*/
