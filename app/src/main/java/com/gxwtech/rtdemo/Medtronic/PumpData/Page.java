package com.gxwtech.rtdemo.Medtronic.PumpData;

/**
 * Created by geoff on 5/13/15.
 *
 * This class was taken from medtronic-android-uploader.
 * This class was written such that the constructors did all the work, which resulted
 * in annoyances such as exceptions during constructors.  I've partially re-written it
 * to do the work in sane fashion.
 *
 */
import android.util.Log;

import com.gxwtech.rtdemo.CRC;
import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.BasalProfileStart;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.BatteryActivity;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Bolus;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.BolusWizard;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.BolusWizardChange;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.CalBgForPh;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ChangeBasalProfile;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ChangeRemoteId;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ChangeTime;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ChangeTimeDisplay;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ChangeUtility;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ClearAlarm;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.EndResultsTotals;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Ian3F;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.LowBattery;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.LowReservoir;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.NewTimeSet;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.NoDeliveryAlarm;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Old6c;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Prime;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.PumpResumed;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.PumpSuspended;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Record;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.RecordTypeEnum;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ResultTotals;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Rewound;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.Sara6E;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.SelectBasalProfile;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.TempBasalDuration;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.TempBasalRate;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.ToggleRemote;
import com.gxwtech.rtdemo.Medtronic.PumpData.records.UnabsorbedInsulin;
import com.gxwtech.rtdemo.Medtronic.PumpModel;

import org.joda.time.DateTime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Page {
    private final static String TAG = "Page";

    private byte[] crc;
    private byte[] data;
    protected PumpModel model;
    public List<Record> mRecordList;

    public Page() {
        this.model = PumpModel.UNSET;
        mRecordList = new ArrayList<>();
    }

    public boolean parseFrom(byte[] rawPage, PumpModel model) {
        mRecordList = new ArrayList<>(); // wipe old contents each time when parsing.
        if (rawPage.length != 1024) {
            Log.e(TAG,"Unexpected page size. Expected: 1024 Was: " + rawPage.length);
            return false;
        }
        this.model = model;
        Log.i(TAG,"Parsing page");
        this.data = Arrays.copyOfRange(rawPage, 0, 1022);
        this.crc = Arrays.copyOfRange(rawPage, 1022, 1024);
        byte[] expectedCrc = CRC.calculate16CCITT(this.data);
        Log.i(TAG, String.format("Data length: %d", data.length));
        if (!Arrays.equals(crc, expectedCrc)) {
            Log.w(TAG, String.format("CRC does not match expected value. Expected: %s Was: %s", HexDump.toHexString(expectedCrc), HexDump.toHexString(crc)));
        } else {
            Log.i(TAG, "CRC OK");
        }

        // Go through page, parsing what we can
        // add records to recordList
        // Records can be of variable size, so ask the record how large it is.
        int dataIndex = 0;
        boolean done = false;
        while(!done) {
            Record record = attemptParseRecord(data, dataIndex);
            if (record != null) {
                mRecordList.add(record);
                Log.d(TAG,String.format("Found record %s at index %d",
                        record.getClass().getSimpleName(),dataIndex));
                // old code stopped when it encountered a 0x00 where it expected a record to start.
                dataIndex = dataIndex + record.getSize(); // jump to next record
            } else {
                Log.d(TAG,String.format("No record found for bytecode 0x%02X",data[dataIndex]));
                // if we failed to form a record, quit.
                done = true;
            }
            if (dataIndex >= data.length) {
                // if we've hit the end of our page, quit.
                done = true;
            } else if (data[dataIndex] == 0x00) {
                // if the next 'record' starts with a zero, quit.
                Log.d(TAG,String.format("Found end of Records(0x00) at index %d",dataIndex));
                done = true;
            }
        }
        Log.i(TAG, String.format("Number of records: %d", mRecordList.size()));
        int index = 1;
        for (Record r : mRecordList) {
            Log.i(TAG, String.format("Record #%d", index));
            r.logRecord();
            index += 1;
        }
        return true;
    }

    /* attemptParseRecord will attempt to create a subclass of Record from the given
     * data and offset.  It will return NULL if it fails.  If it succeeds, the returned
     * subclass of Record can be examined for its length, so that the next attempt can be made.
     */
    public static <T extends Record> T attemptParseRecord(byte[] data, int offsetStart) {
        // no data?
        if (data == null) {
            return null;
        }
        // invalid offset?
        if (data.length < offsetStart) {
            return null;
        }
        //Log.d(TAG,String.format("checking for handler for record type 0x%02X at index %d",data[offsetStart],offsetStart));
        RecordTypeEnum en = RecordTypeEnum.fromByte(data[offsetStart]);
        T record = en.getRecordClass();
        return record;
    }

    public static DateTime parseSimpleDate(byte[] data, int offset) {
        DateTime timeStamp = null;
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
        /*
        Log.w(TAG, String.format("Attempting to create DateTime from: %04d-%02d-%02d %02d:%02d:%02d",
                year + 2000, month, dayOfMonth, hour, minutes, seconds));
         */
        try {
            timeStamp = new DateTime(year + 2000, month, dayOfMonth, hour, minutes, seconds);
        } catch (org.joda.time.IllegalFieldValueException e) {
            //Log.e(TAG,"Illegal DateTime field");
            //e.printStackTrace();
            return null;
        }
        return timeStamp;
    }

    public static void discoverRecords(byte[] data) {
        int i = 0;
        boolean done = false;

        ArrayList<Integer> keyLocations= new ArrayList();
        while (!done) {
            RecordTypeEnum en = RecordTypeEnum.fromByte(data[i]);
            if (en != RecordTypeEnum.RECORD_TYPE_NULL) {
                keyLocations.add(i);
                Log.w(TAG,String.format("Possible record of type %s found at index %d", en, i));
            }
            /*
            DateTime ts = parseSimpleDate(data,i);
            if (ts != null) {
                if (ts.year().get() == 2015) {
                    Log.w(TAG, String.format("Possible simple date at index %d", i));
                }
            }
            */
            i = i + 1;
            done = (i >= data.length-2);
        }
        // for each of the discovered key locations, attempt to parse a sequence of records
        for(RecordTypeEnum en : RecordTypeEnum.values()) {

        }
        for (int ix = 0; ix < keyLocations.size(); ix++) {

        }
    }

}