package com.gxwtech.rtdemo;

import android.content.Context;
import android.util.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoTimeoutException;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by geoff on 4/16/15.
 */
public class MongoWrapper {
    private static final String TAG = "MongoWrapper";
    public PersistentBoolean allowWritingToDB;
    protected String mURIString = "<MongoDB URI string - uninitialized>";
    protected String mDBName = "<MongoDB database name - uninitialized>";
    protected String mCollection = "entries"; // cgm readings
    protected String mTreatmentsCollectionName = "treatments"; // treatments (Temp Basals/carb corrections)


    protected boolean setupCompleted = false;
    MongoClientURI mUri = null;
    MongoClient mMongoClient = null;
    DB mDB = null;

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

    public MongoWrapper(Context ctx) {
        allowWritingToDB = new PersistentBoolean(ctx.getSharedPreferences(Constants.PreferenceID.MainActivityPrefName, 0),
                Constants.PrefName.MongoDBAllowWritingToDBPrefName,true);

    }

    public void checkSetup() throws java.net.UnknownHostException {
        if (setupCompleted) return;
        /*
        The format of the URI is:
        mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
        */
        Log.i(TAG,"Mongo Client URI is:" + mURIString);
        mUri = new MongoClientURI(mURIString);
        mMongoClient = new MongoClient(mUri);
        mDB = mMongoClient.getDB(mDBName);

        setupCompleted = true;
    }

    public void updateURI(String serverAddress, String serverPort, String dbname, String username,
                          String password, String collection) {
        mURIString = "mongodb://"+username+":"+password+"@"+serverAddress+":"+serverPort+"/"+dbname;
        mDBName = dbname;
        mCollection = collection;
    }

    public List<DBTempBasalEntry> downloadRecentTreatments(int maxAgeMinutes) {
        ArrayList<DBTempBasalEntry> treatmentList = new ArrayList<>();
        try {
            checkSetup(); // if we haven't set up the DB connection, do so.
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
            return null;
        }

        DBCollection coll = mDB.getCollection("treatments");
        // remember to use mongoClient.close()...
        Log.d(TAG,"Getting TempBasal records from MongoDB");
        int recordCount = 0;
        Long recordSearchStartTime = Instant.now().getMillis() - (maxAgeMinutes * 60 * 1000);
        BasicDBObject query = new BasicDBObject("date", new BasicDBObject("$gt",recordSearchStartTime));
        DBCursor cursor = coll.find(query);
        try {
            while (cursor.hasNext()) {
                // get a record
                DBObject obj = cursor.next();
                // parse it
                if (obj.containsField("enteredBy")) {
                    String etype = (String) obj.get("enteredBy");
                    if (etype.equals(DBTempBasalEntry.enteredBy)) {
                        // it's one of ours, parse the rest.
                        DBTempBasalEntry newEntry = new DBTempBasalEntry();
                        newEntry.readFromDBObject(obj);
                        treatmentList.add(newEntry);
                    }
                } else {
                    Log.d(TAG,"Unknown entry in MongoDB collection '" + mTreatmentsCollectionName + "'");
                }
            }
        } catch (MongoCommandException e) {
            e.printStackTrace();
        } catch (com.mongodb.MongoTimeoutException e) {
            Log.e(TAG, "MongoDB connection timeout");
        } finally {
            cursor.close();
        }
        return treatmentList;
    }

    public void uploadTreatment(DBTempBasalEntry entry) {
        DBCollection coll = mDB.getCollection("treatments");
        coll.insert(entry.formatDBObject());
    }

    public BGReadingResponse getBGReading() throws com.mongodb.MongoTimeoutException
    {
        BGReadingResponse response = new BGReadingResponse();
        try {
            checkSetup(); // if we haven't set up the DB connection, do so.
        } catch (java.net.UnknownHostException e) {
            response.setError("MongoDB: Unknown host");
            e.printStackTrace();
            return response;
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
            response.setError(e.toString());
            return response;

        }

        DBCollection coll = mDB.getCollection(mCollection);
        // remember to use mongoClient.close()...
        Log.d(TAG,"Getting BG reading from MongoDB");
        int recordCount = 0;
        Long millisecondsAtTwentyMinutesAgo = Instant.now().getMillis() - (20 * 60 * 1000);
        BasicDBObject query = new BasicDBObject("date", new BasicDBObject("$gt",millisecondsAtTwentyMinutesAgo));
        DBCursor cursor = coll.find(query);
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
                    Long millisecondsSince1970 = (Long)(obj.get("date"));
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

                        Log.i(TAG, String.format("Found record: Timestamp %s, bg: %.2f",
                                reading.mTimestamp.toString(), reading.mBg));


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
                recordCount += 1;
            }
            response.reading = latestBGReading;
            Log.i(TAG,"Total MongoDB entries read: " + recordCount);
            // android says commandFailureException is deprecated, but it's what's thrown...
        } catch (MongoCommandException e) {
            response.setError(e.toString());
            e.printStackTrace();
        } catch (MongoTimeoutException e) {
            response.setError("MongoDB connection timeout");
            Log.e(TAG, "MongoDB connection timeout");
        } finally {
            cursor.close();
        }

        if (latestBGReading != null) {
            // This is the latest reading, though it may not be new to us.  Let APSLogic handle that.
            Log.i(TAG, String.format("BG Reading from MongoDB: Timestamp %s, bg: %.2f",
                    latestBGReading.mTimestamp.toString(), latestBGReading.mBg));
        } else {
            response.setError("Zero records found");
        }

        return response;
    }
}
