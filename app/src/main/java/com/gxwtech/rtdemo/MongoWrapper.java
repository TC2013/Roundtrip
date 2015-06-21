package com.gxwtech.rtdemo;

import android.util.Log;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;


/**
 * Created by geoff on 4/16/15.
 */
public class MongoWrapper {
    private static final String TAG = "MongoWrapper";
    protected String mURI = "<MongoDB URI string - uninitialized>";
    protected String mDBName = "<MongoDB database name - uninitialized>";
    protected String mCollection = "entries";
    // todo: I don't like inner classes.  Move to a new class
    public class BGReadingResponse {
        public BGReading reading = new BGReading();
        public boolean error = false;
        public String errorMessage = "";
        public BGReadingResponse() {
            reading = new BGReading();
            error = false;
            errorMessage = "";
        }
        public void setError(String message) {
            error = true;
            errorMessage = message;
        }
    }
    public MongoWrapper() {
    }

    public void updateURI(String serverAddress, String serverPort, String dbname, String username,
                          String password, String collection) {
        mURI = "mongodb://"+username+":"+password+"@"+serverAddress+":"+serverPort+"/"+dbname;
        mDBName = dbname;
        mCollection = collection;
    }

    public BGReadingResponse getBGReading() throws com.mongodb.MongoTimeoutException
    {
        BGReadingResponse response = new BGReadingResponse();
        /*
        The format of the URI is:
        mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
        */
        Log.i(TAG,"Mongo Client URI is:" + mURI);
        MongoClientURI uri = new MongoClientURI(mURI);
        MongoClient mongoClient = null;
        try {
            mongoClient = new MongoClient(uri);
        } catch (java.net.UnknownHostException e) {
            response.setError("MongoDB: Unknown host");
            e.printStackTrace();
            return response;
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
            response.setError(e.toString());
            return response;

        }
        DB db = mongoClient.getDB(mDBName);
        DBCollection coll = db.getCollection(mCollection);
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
                            response.setError("Unable to parse date string format: " + dateString);
                            Log.e(TAG, "Unable to parse date string format: " + dateString);
                        }
                    }
                    // sometimes, sgv comes in as an Integer, sometimes as a Long... yay...
                    if (bgTimestamp != null) {
                        int bg = Integer.parseInt(obj.get("sgv").toString());
                        BGReading reading;
                        reading = new BGReading(bgTimestamp,(double)bg);
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
            response.reading = latestBGReading;
            // android says commandFailureException is deprecated, but it's what's thrown...
        } catch (com.mongodb.CommandFailureException e) {
            response.setError(e.toString());
            e.printStackTrace();
        } catch (com.mongodb.MongoTimeoutException e) {
            response.setError("MongoDB connection timeout");
            Log.e(TAG, "MongoDB connection timeout");
        } finally {
            cursor.close();
        }

        if (latestBGReading != null) {
            // This is the latest reading, though it may not be new to us.  Let APSLogic handle that.
            Log.i(TAG, String.format("BG Reading from MongoDB: Timestamp %s, bg: %.2f",
                    latestBGReading.mTimestamp.toString(), latestBGReading.mBg));
        }

        return response;
    }
}
