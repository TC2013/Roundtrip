package com.gxwtech.rtdemo;

import android.util.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Arrays;


/**
 * Created by geoff on 4/16/15.
 */
public class MongoWrapper {
    private static final String TAG = "MongoWrapper";
    public MongoWrapper() {
    }

    public BGReading getBGReading() {
        /*
        The format of the URI is:
        mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
        */
        //String userName = "tobycanning";
        String userName = "roundtrip";
        String database = "db";
        //String password = "kardia01";
        String password = "makertgo";
        MongoClientURI uri = new MongoClientURI("mongodb://roundtrip:makertgo@ds031952.mongolab.com:31952/db");
        MongoClient mongoClient = null;
        try {
            mongoClient = new MongoClient(uri);
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        DB db = mongoClient.getDB("db");
        DBCollection coll = db.getCollection("entries");
        // remember to use mongoClient.close()...

        int i = 0;
        DBCursor cursor = coll.find();
        BGReading latestBGReading = null;
        Duration latestDiff = null;
        try {
            while (cursor.hasNext()) {
                // we want:
                // date: seconds since epoch (good for indexing)
                // datestring: better date string -- get timestamp from here
                // sgv: the BG reading
                //Log.i(TAG, String.format("Document #%d: %s", i, cursor.next().toString()));
                // For AR regression, we will want last 30 min (more?)
                // for now, grab last one.
                DBObject obj = cursor.next();
                if (obj.containsField("sgv")) {
                    Long secondsSince1970 = (Long)(obj.get("date"));
                    String dateString = (String)(obj.get("dateString"));
                    // When reading the DB, I see two formats of dateString:
                    //dateString:2015-05-30T18:23:07.272-05:00
                    //dateString:05/30/2015 18:38:06 PM
                    DateTime bgTimestamp = null;
                    DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss a");
                    try {
                        bgTimestamp = formatter.parseDateTime(dateString);
                    } catch (IllegalArgumentException e) {
                        formatter = ISODateTimeFormat.dateTimeParser();
                        try {
                            bgTimestamp = formatter.parseDateTime(dateString);
                        } catch (IllegalArgumentException e2) {
                            bgTimestamp = null; // ran out of ideas.
                            Log.e(TAG,"Unable to parse date string format: " + dateString);
                        }
                    }
                    // sometimes, sgv comes in as an Integer, sometimes as a Long... yay...
                    if (bgTimestamp != null) {
                        int bg = Integer.parseInt(obj.get("sgv").toString());
                        BGReading reading = new BGReading(bgTimestamp,(double)bg);
                        /*
                        Log.i(TAG, String.format("Found record: Timestamp %s, bg: %.2f",
                                reading.mTimestamp.toString(), reading.mBg));
                        */

                        DateTime now = DateTime.now();
                        if (latestBGReading != null) {
                            Duration diff = new Duration(reading.mTimestamp, now);
                            if (diff.getStandardSeconds() < latestDiff.getStandardSeconds()) {
                                latestBGReading = reading;
                                latestDiff = diff;
                            }
                        } else {
                            latestBGReading = reading;
                            latestDiff = new Duration(reading.mTimestamp, now);
                        }
                    }
                }
                i = i + 1;
            }
            // android says commandFailureException is deprecated, but it's what's thrown...
        } catch (com.mongodb.CommandFailureException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }

        if (latestBGReading != null) {
            // This is the latest reading, though it may not be new to us.  Let APSLogic handle that.
            Log.i(TAG, String.format("BG Reading from MongoDB: Timestamp %s, bg: %.2f",
                    latestBGReading.mTimestamp.toString(), latestBGReading.mBg));
        }
        return latestBGReading;
    }

}
