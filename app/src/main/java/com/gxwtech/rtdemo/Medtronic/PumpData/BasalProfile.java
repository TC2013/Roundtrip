package com.gxwtech.rtdemo.Medtronic.PumpData;

import android.util.Log;

import org.joda.time.LocalTime;

import java.util.ArrayList;

/**
 * Created by geoff on 6/1/15.
 *
 * There are three basal profiles stored on the pump. (722 only?)
 * They are all parsed the same, the user just has 3 to choose from:
 * Standard, A, and B
 *
 * The byte array seems to be 21 three byte entries long, plus a zero?
 * If the profile is completely empty, it should have one entry: [0,0,0x3F] (?)
 * The first entry of [0,0,0] marks the end of the used entries.
 *
 * Each entry is assumed to span from the specified start time to the start time of the
 * next entry, or to midnight if there are no more entries.
 *
 * Individual entries are of the form [r,z,m] where
 * r is the rate (in 0.025 U increments)
 * z is zero (?)
 * m is the start time-of-day for the basal rate period (in 30 minute increments?)
 */
public class BasalProfile {
    private static final String TAG = "BasalProfile";
    protected static final int MAX_RAW_DATA_SIZE = (21 * 3) + 1;
    protected byte[] mRawData; // store as byte array to make transport (via parcel) easier
    public BasalProfile() {
        init();
    }
    public void init() {
        mRawData = new byte[MAX_RAW_DATA_SIZE];
        mRawData[0] = 0;
        mRawData[1] = 0;
        mRawData[2] = 0x3f;
    }
    public boolean setRawData(byte[] data) {
        if (data == null) {
            Log.e(TAG,"setRawData: buffer is null!");
            return false;
        }
        int len = Math.min(MAX_RAW_DATA_SIZE, data.length);
        System.arraycopy(data, 0, mRawData, 0, len);
        Log.w(TAG,String.format("setRawData: copied raw data buffer of %d bytes.",len));
        return true;
    }

    public void dumpBasalProfile() {
        Log.w(TAG,"Basal Profile entries:");
        ArrayList<BasalProfileEntry> entries = getEntries();
        for (int i=0; i< entries.size(); i++) {
            BasalProfileEntry entry = entries.get(i);
            String startString = entry.startTime.toString("HH:mm");
            Log.w(TAG,String.format("Entry %d, rate=%.3f (0x%02X), start=%s (0x%02X)",
                    i+1, entry.rate, entry.rate_raw,
                    startString, entry.startTime_raw));

        }
    }

    public BasalProfileEntry getEntryForTime(LocalTime lt) {
        BasalProfileEntry rval = new BasalProfileEntry();
        ArrayList<BasalProfileEntry> entries = getEntries();
        if (entries.size() == 0) {
            Log.w(TAG,String.format("getEntryForTime(%s): table is empty",lt.toString("HH:mm")));
            return rval;
        }
        int localMillis = lt.getMillisOfDay();
        boolean done = false;
        int i=0;
        while (!done) {
            BasalProfileEntry entry = entries.get(i);
            if (localMillis < entry.startTime.getMillisOfDay()) {
                rval = entry;
                done = true;
            }
            i++;
            if (i >= entries.size()) {
                done = true;
            }
        }
        Log.w(TAG,String.format("getEntryForTime(%s): Returning entry %d: rate=%.3f (%d), start=%s (%d)",
                lt.toString("HH:mm"),i,
                rval.rate,rval.rate_raw,
                rval.startTime.toString("HH:mm"),rval.startTime_raw));
        return rval;
    }

    public ArrayList<BasalProfileEntry> getEntries() {
        ArrayList<BasalProfileEntry> entries = new ArrayList<>();
        if (mRawData[2] == 0x3f) {
            Log.w(TAG,"Raw Data is empty.");
            return entries; // an empty list
        }
        int i = 0;
        boolean done = false;
        byte r,st;
        while (!done) {
            r = mRawData[i];
            st = mRawData[i+2];
            entries.add(new BasalProfileEntry(r,st));
            i=i+3;
            if (i>=21) {
                done=true;
            } else if ((mRawData[i]==0) && (mRawData[i+1]==0) && (mRawData[i+2]==0)) {
                done = true;
            }
        }
        return entries;
    }

    public static void testParser() {
    byte[] testData = new byte[] {
            32, 0, 0,
            38, 0, 13,
            44, 0, 19,
            38, 0, 28,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0 };
  /* from decocare:
  _test_schedule = {'total': 22.50, 'schedule': [
    { 'start': '12:00A', 'rate': 0.80 },
    { 'start': '6:30A', 'rate': 0.95 },
    { 'start': '9:30A', 'rate': 1.10 },
    { 'start': '2:00P', 'rate': 0.95 },
  ]}
  */
        BasalProfile profile = new BasalProfile();
        profile.setRawData(testData);
        ArrayList<BasalProfileEntry> entries = profile.getEntries();
        if (entries.isEmpty()) {
            Log.e(TAG,"testParser: failed");
        } else {
            for (int i=0; i<entries.size(); i++) {
                BasalProfileEntry e = entries.get(i);
                Log.d(TAG,String.format("testParser entry #%d: rate: %.2f, start %d:%d",
                        i,e.rate,e.startTime.getHourOfDay(),
                        e.startTime.getMinuteOfHour()));
            }
        }

    }
}
