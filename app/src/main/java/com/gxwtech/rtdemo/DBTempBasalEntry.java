package com.gxwtech.rtdemo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by geoff on 6/30/15.
 */
public class DBTempBasalEntry {
    public static String enteredBy = "Roundtrip";
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

    public BasicDBObject formatDBObject() {
        BasicDBObject myDBObj = new BasicDBObject("enteredBy",enteredBy)
                .append("eventType",eventType)
                .append("date",mTimestamp.getMillis())
                //.append("date", String.format("%d", mTimestamp.getMillis()))
                .append("insulin", String.format("%.3f", mRelativeInsulin))
                .append("durationMin", String.format("%d", mDurationMinutes))
                .append("created_at", mTimestamp.toDateTime(DateTimeZone.UTC).toString())
                .append("notes", "Start: " + startTime + "\nEnd: " + endTime + "\n");
        return myDBObj;

    }

    public void readFromDBObject(DBObject obj) {
        Long millisecondsSince1970 = (Long) (obj.get("date"));
        //mTimestamp = new DateTime(millisecondsSince1970);
        // Note: we still need the millisecondsSince1970 "date" object in the db because we key times off it.
        mTimestamp = DateTime.parse((String)obj.get("created_at"));
        mDurationMinutes = Integer.parseInt((String)(obj.get("durationMin")));
        mRelativeInsulin = Double.parseDouble((String)(obj.get("insulin")));
        calcStartEndTimes();
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