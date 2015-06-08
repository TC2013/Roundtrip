package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpModel;

import org.joda.time.DateTime;


abstract public class TimeStampedRecord extends Record {
    private final static String TAG = "TimeStampedRecord";

    protected DateTime timeStamp;

    public TimeStampedRecord() {
        timestampSize = 5;
        timeStamp = new DateTime(0); // an 'invalid' DateTime
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        if (!super.collectRawData(data,model)) {
            return false;
        }
        return true; // don't decode yet. Subclass does that.
    }

    protected boolean decode(byte[] data) {
        return parseDate(data,headerSize);
    }

    /*
     * There seem to be two codes:
     * a 2 byte date code, and a 5 byte date-time code. Yes?
     * Short date used when?
     */

    protected boolean parseShortDate(byte[] data, int offset) {
        int seconds = 0;
        int minutes = 0;
        int hour = 0;
        //int high = data[0] >> 4;
        int low = data[0 + offset] & 0x1F;
        //int year_high = data[1] >> 4;
        int mhigh = (data[0 + offset] & 0xE0) >> 4;
        int mlow = (data[1 + offset] & 0x80) >> 7;
        int month = mhigh + mlow;
        int dayOfMonth = low + 1;
        // python code says year is data[1] & 0x0F, but that will cause problem in 2016.
        // Hopefully, the remaining bits are part of the year...
        int year = data[1 + offset] & 0x3F;
        Log.w(TAG, String.format("Attempting to create DateTime from: %04d-%02d-%02d %02d:%02d:%02d",
                year + 2000, month, dayOfMonth, hour, minutes, seconds));
        try {
            timeStamp = new DateTime(year + 2000, month, dayOfMonth, hour, minutes, seconds);
        } catch (org.joda.time.IllegalFieldValueException e) {
            Log.e(TAG,"Illegal DateTime field");
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    // for relation to old code, replace offset with headerSize
    protected boolean parseDate(byte[] data, int offset){
        //offset = headerSize;
        Log.w(TAG,String.format("bytes to parse: 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X",
                data[offset], data[offset+1], data[offset+2], data[offset+3], data[offset+4]));
        int seconds = data[offset] & 0x3F;
        int minutes = data[offset + 1] & 0x3F;
        int hour = data[offset + 2] & 0x1F;
        int dayOfMonth = data[offset + 3] & 0x1F;
        // Yes, the month bits are stored in the high bits above seconds and minutes!!
        int month = ((data[offset] & 0xC0) >> 4) | ((data[offset + 1] & 0xC0) >> 6);
        int year = data[offset + 4] & 0x3F; // Assuming this is correct, need to verify. Otherwise this will be a problem in 2016.
        Log.w(TAG,String.format("Attempting to create DateTime from: %04d-%02d-%02d %02d:%02d:%02d",
                year+2000,month,dayOfMonth,hour,minutes,seconds));
        try {
            timeStamp = new DateTime(year + 2000, month, dayOfMonth, hour, minutes, seconds);
        } catch (org.joda.time.IllegalFieldValueException e) {
            Log.e(TAG,"Illegal DateTime field");
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public DateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public void logRecord() {
        Log.i(TAG,String.format("Time stamped record (%s): %s", recordTypeName, timeStamp.toString()));
    }
}
