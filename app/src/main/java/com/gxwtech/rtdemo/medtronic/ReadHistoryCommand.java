package com.gxwtech.rtdemo.medtronic;

import android.util.Log;

import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.medtronic.PumpData.HistoryReport;
import com.gxwtech.rtdemo.medtronic.PumpData.Page;
import com.gxwtech.rtdemo.medtronic.PumpData.TempBasalPair;
import com.gxwtech.rtdemo.medtronic.PumpData.records.BolusWizard;
import com.gxwtech.rtdemo.medtronic.PumpData.records.Record;
import com.gxwtech.rtdemo.medtronic.PumpData.records.RecordTypeEnum;
import com.gxwtech.rtdemo.medtronic.PumpData.records.TempBasalDuration;
import com.gxwtech.rtdemo.medtronic.PumpData.records.TempBasalRate;

/**
 * Created by Geoff on 5/2/2015.
 */
public class ReadHistoryCommand extends MedtronicCommand {
    private static final String TAG = "ReadHistoryCommand";
    private static final boolean DEBUG_READHISTORYCOMMAND = false;
    public boolean mParsedOK;
    // HistoryReport to collect the valuable things from the history (as we cannot yet parse all)
    public HistoryReport mHistoryReport;

    public ReadHistoryCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_HISTORY);
        mHistoryReport = new HistoryReport();
        mNRetries = 2;
        mMaxRecords = 2;
        mSleepForPumpResponse = 100; // must adjust these numbers to get data from carelink?
        mSleepForPumpRetry = 500;
        mParams = new byte[]{0x00}; // This is the page number
        mParsedOK = false;

    }

    public void setPageNumber(int pageNumber) {
        mParams[0] = (byte) pageNumber;
    }

    private static void checkForRecordSequence(byte[] data, int index) {
        Record r;
        int seq = 0;
        r = checkForRecord(data, index);
        while (r != null) {
            seq++;
            if (seq > 1) {
                // note sequential discoveries
                if (DEBUG_READHISTORYCOMMAND) {
                    Log.w(TAG, String.format("SEQ: %d", seq));
                }
            }
            index = index + r.getSize();
            r = checkForRecord(data, index);
        }
    }

    private static Record checkForRecord(byte[] data, int index) {
        Page page = new Page();
        Record record = page.attemptParseRecord(data, index); // CalBgForPh
        if (record != null) {
            if (DEBUG_READHISTORYCOMMAND) {
                Log.d(TAG, String.format("Maybe found record %s at index %d, size %d",
                        record.getClass().getSimpleName(), index, record.getSize()));
            }

            int sublength = data.length - index;
            byte[] subset = new byte[sublength];
            System.arraycopy(data, index, subset, 0, sublength);
            boolean correct = record.collectRawData(subset, PumpModel.MM522);
            if (correct) {
                if (DEBUG_READHISTORYCOMMAND) {
                    Log.v(TAG, String.format("FOUND RECORD %s at index %d, size %d",
                            record.getClass().getSimpleName(), index, record.getSize()));
                }
            } else {
                /*
                Log.e(TAG, String.format("Failed to load record %s at index %d, size %d",
                        record.getClass().getSimpleName(), index, record.getSize()));
                        */
            }
        } else {
            if (DEBUG_READHISTORYCOMMAND) {
                Log.d(TAG, String.format("NO RECORD FOUND at index %d, code 0x%02X", index, data[index]));
            }

        }
        return record;
    }

    public void parse(byte[] receivedData) {
        if (receivedData != null) {
            if (DEBUG_READHISTORYCOMMAND) {
                Log.w(TAG, "parse: " + HexDump.dumpHexString(receivedData));
            }
            int d = receivedData.length;
            if (d == 1024) {
                //Log.e(TAG, "******* Begin record discovery ******");
                //discoverRecords(receivedData);
                //Log.e(TAG,"******* END record discovery ********");
                Page page = new Page();
                mParsedOK = page.parseFrom(receivedData, PumpModel.MM522); // todo: fix hardcoded pump model
                // Here we select the records of the page which will go into the report
                TempBasalEvent partialTempBasalEvent = null;
                for (Record r : page.mRecordList) {
                    if (r.getRecordOp() == RecordTypeEnum.RECORD_TYPE_BOLUSWIZARD.opcode()) {
                        mHistoryReport.addBolusWizardEvent((BolusWizard) r);
                        if (DEBUG_READHISTORYCOMMAND) {
                            Log.d(TAG, "Adding BolusWizard event to HistoryReport");
                        }
                    }
                    /* temp basal events are divided into two sub-events, which we have to re-compose.
                     * One half is a TempBasalRate, and the other half is TempBasalDuration
                     * We re-compose them into a TempBasalEvent here.
                     */
                    else if (r.getRecordOp() == RecordTypeEnum.RECORD_TYPE_TEMPBASALRATE.opcode()) {
                        // need to find corresponding duration event
                        TempBasalRate rateEvent = (TempBasalRate) r;
                        if (partialTempBasalEvent == null) {
                            partialTempBasalEvent = new TempBasalEvent(rateEvent.getTimeStamp(),
                                    new TempBasalPair(rateEvent.basalRate, 0));
                        } else {
                            if (partialTempBasalEvent.mTimestamp.equals(rateEvent.getTimeStamp())) {
                                // found matching rate/duration
                                partialTempBasalEvent.mBasalPair.mInsulinRate = rateEvent.basalRate;
                                // and record the full event
                                mHistoryReport.addTempBasalEvent(partialTempBasalEvent);
                                partialTempBasalEvent = null;
                            } else {
                                Log.e(TAG, "TempBasalDuration/TempBasalRate timestamp mismatch!");
                                // try to recover
                                partialTempBasalEvent = new TempBasalEvent(rateEvent.getTimeStamp(),
                                        new TempBasalPair(rateEvent.basalRate, 0));
                            }
                        }
                    } else if (r.getRecordOp() == RecordTypeEnum.RECORD_TYPE_TEMPBASALDURATION.opcode()) {
                        // need to find corresponding rate event
                        TempBasalDuration durationEvent = (TempBasalDuration) r;
                        if (partialTempBasalEvent == null) {
                            partialTempBasalEvent = new TempBasalEvent(durationEvent.getTimeStamp(),
                                    new TempBasalPair(0, durationEvent.durationMinutes));
                        } else {
                            if (partialTempBasalEvent.mTimestamp.equals(durationEvent.getTimeStamp())) {
                                // found matching event, update partial
                                partialTempBasalEvent.mBasalPair.mDurationMinutes = durationEvent.durationMinutes;
                                // record finished event
                                mHistoryReport.addTempBasalEvent(partialTempBasalEvent);
                                partialTempBasalEvent = null;
                            } else {
                                Log.e(TAG, "TempBasalRate/TempBasalDuration timestamp mismatch!");
                                // try to recover
                                partialTempBasalEvent = new TempBasalEvent(durationEvent.getTimeStamp(),
                                        new TempBasalPair(0, durationEvent.durationMinutes));
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, String.format("Cannot decode page of invalid size %d (should be 1024)", d));
                mParsedOK = false;
            }
        }
    }


    public static void discoverRecords(byte[] data) {
        int i = 0;
        boolean done = false;

        while (!done) {
            RecordTypeEnum en = RecordTypeEnum.fromByte(data[i]);
            if (en != RecordTypeEnum.RECORD_TYPE_NULL) {
                Log.v(TAG, String.format("Possible record of type %s found at index %d", en, i));
                checkForRecordSequence(data, i);
            }
            i = i + 1;
            done = (i >= data.length - 2);
        }
    }


    public void testParser() {
        byte[] sampleHistory = new byte[]{
                (byte) 0x6d, (byte) 0x46, (byte) 0x8f, (byte) 0x05, (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x3a, (byte) 0x64,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00,
                (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x00, (byte) 0x4d, (byte) 0x6f,
                (byte) 0x0b, (byte) 0x07, (byte) 0x0f, (byte) 0x3d, (byte) 0x75, (byte) 0x16, (byte) 0x17, (byte) 0xa7,
                (byte) 0x8d, (byte) 0xcf, (byte) 0x3e, (byte) 0xc0, (byte) 0x0c, (byte) 0x80, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x26, (byte) 0x01, (byte) 0x42, (byte) 0x70, (byte) 0x0b, (byte) 0x07, (byte) 0x0f,
                (byte) 0x27, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x28,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x26, (byte) 0x00,
                (byte) 0x45, (byte) 0x70, (byte) 0x0b, (byte) 0x07, (byte) 0x0f, (byte) 0x27, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1b, (byte) 0x03, (byte) 0x65, (byte) 0x70, (byte) 0x0b,
                (byte) 0x07, (byte) 0x0f, (byte) 0x1b, (byte) 0x07, (byte) 0x4d, (byte) 0x71, (byte) 0x0b, (byte) 0x07,
                (byte) 0x0f, (byte) 0x0a, (byte) 0x69, (byte) 0x4e, (byte) 0x4c, (byte) 0x2c, (byte) 0x07, (byte) 0x0f,
                (byte) 0x5b, (byte) 0x69, (byte) 0x5f, (byte) 0x4c, (byte) 0x0c, (byte) 0x07, (byte) 0x0f, (byte) 0x17,
                (byte) 0x50, (byte) 0x09, (byte) 0x32, (byte) 0x64, (byte) 0x00, (byte) 0x19, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x19, (byte) 0x6e, (byte) 0x01, (byte) 0x19, (byte) 0x19, (byte) 0x00,
                (byte) 0x5f, (byte) 0x4c, (byte) 0x4c, (byte) 0x07, (byte) 0x0f, (byte) 0x06, (byte) 0x06, (byte) 0x09,
                (byte) 0xb3, (byte) 0x40, (byte) 0x4c, (byte) 0x53, (byte) 0xa7, (byte) 0x0f, (byte) 0x0c, (byte) 0x06,
                (byte) 0x5a, (byte) 0x4e, (byte) 0x13, (byte) 0x07, (byte) 0x0f, (byte) 0x07, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x9e, (byte) 0x47, (byte) 0x8f, (byte) 0x6d, (byte) 0x47, (byte) 0x8f, (byte) 0x05,
                (byte) 0x00, (byte) 0x69, (byte) 0x69, (byte) 0x69, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x9e, (byte) 0x00, (byte) 0x3a, (byte) 0x25, (byte) 0x00, (byte) 0x64, (byte) 0x3f, (byte) 0x00,
                (byte) 0x17, (byte) 0x00, (byte) 0x64, (byte) 0x3f, (byte) 0x00, (byte) 0x64, (byte) 0x64, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x06, (byte) 0x06, (byte) 0x09, (byte) 0xb3, (byte) 0x40, (byte) 0x4e, (byte) 0x42, (byte) 0xa8,
                (byte) 0x0f, (byte) 0x0c, (byte) 0x06, (byte) 0x6d, (byte) 0x5c, (byte) 0x02, (byte) 0x08, (byte) 0x0f,
                (byte) 0x1b, (byte) 0x00, (byte) 0x44, (byte) 0x5d, (byte) 0x02, (byte) 0x08, (byte) 0x0f, (byte) 0x07,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x48, (byte) 0x8f, (byte) 0x6d, (byte) 0x48,
                (byte) 0x8f, (byte) 0x05, (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x00, (byte) 0x38, (byte) 0x64, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a, (byte) 0x49,
                (byte) 0x8f, (byte) 0x6d, (byte) 0x49, (byte) 0x8f, (byte) 0x05, (byte) 0x0c, (byte) 0x00, (byte) 0xe8,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x3a,
                (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c,
                (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x3a, (byte) 0x4a, (byte) 0x8f, (byte) 0x6d, (byte) 0x4a, (byte) 0x8f, (byte) 0x05,
                (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x3a, (byte) 0x00, (byte) 0x3a, (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a, (byte) 0x4b, (byte) 0x8f, (byte) 0x6d,
                (byte) 0x4b, (byte) 0x8f, (byte) 0x05, (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x3a, (byte) 0x64, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0xe8,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a,
                (byte) 0x4c, (byte) 0x8f, (byte) 0x6d, (byte) 0x4c, (byte) 0x8f, (byte) 0x05, (byte) 0x0c, (byte) 0x00,
                (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3a, (byte) 0x00,
                (byte) 0x3a, (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x0c, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x69,
                (byte) 0x43, (byte) 0x57, (byte) 0x2a, (byte) 0x0d, (byte) 0x0f, (byte) 0x5b, (byte) 0x69, (byte) 0x54,
                (byte) 0x57, (byte) 0x0a, (byte) 0x0d, (byte) 0x0f, (byte) 0x0f, (byte) 0x50, (byte) 0x09, (byte) 0x32,
                (byte) 0x64, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
                (byte) 0x6e, (byte) 0x01, (byte) 0x10, (byte) 0x10, (byte) 0x00, (byte) 0x54, (byte) 0x57, (byte) 0x4a,
                (byte) 0x0d, (byte) 0x0f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0xc4
        };
        Page.discoverRecords(sampleHistory);
        parse(sampleHistory);
        Page page = new Page();
        checkForRecordSequence(sampleHistory, 0);
        checkForRecordSequence(sampleHistory, 4);
        checkForRecordSequence(sampleHistory, 15);
        checkForRecordSequence(sampleHistory, 38);
        checkForRecordSequence(sampleHistory, 49);
        checkForRecordSequence(sampleHistory, 53);
        checkForRecordSequence(sampleHistory, 54);
        checkForRecordSequence(sampleHistory, 60);
        checkForRecordSequence(sampleHistory, 65);
        checkForRecordSequence(sampleHistory, 66);
        checkForRecordSequence(sampleHistory, 70);
        checkForRecordSequence(sampleHistory, 72);
        checkForRecordSequence(sampleHistory, 86);
        checkForRecordSequence(sampleHistory, 91);
        checkForRecordSequence(sampleHistory, 93);
        checkForRecordSequence(sampleHistory, 108);
        checkForRecordSequence(sampleHistory, 112);
        checkForRecordSequence(sampleHistory, 115);
        checkForRecordSequence(sampleHistory, 119);
        checkForRecordSequence(sampleHistory, 121); // CalBgForPh
        checkForRecordSequence(sampleHistory, 128); // BolusWizard
        checkForRecordSequence(sampleHistory, 148); // Bolus
        checkForRecordSequence(sampleHistory, 502); // CalBgForPh
        checkForRecordSequence(sampleHistory, 509); // BolusWizard
        checkForRecordSequence(sampleHistory, 529); // Bolus
    }


}

/*
Cribbed from:
https://github.com/nightscout/android-uploader/tree/minimed/core/src/main/java/com/nightscout/core/drivers/Medtronic

        recordMap.put((byte) 0x01, Bolus.class);
        recordMap.put((byte) 0x03, Prime.class);
        recordMap.put((byte) 0x06, NoDeliveryAlarm.class);
        recordMap.put((byte) 0x07, EndResultsTotals.class);
        recordMap.put((byte) 0x08, ChangeBasalProfile.class);
        recordMap.put((byte) 0x09, ChangeBasalProfile.class);
        recordMap.put((byte) 0x0A, CalBgForPh.class);
        recordMap.put((byte) 0x0c, ClearAlarm.class);
        recordMap.put((byte) 0x14, SelectBasalProfile.class);
        recordMap.put((byte) 0x16, TempBasalDuration.class);
        recordMap.put((byte) 0x17, ChangeTime.class);
        recordMap.put((byte) 0x18, NewTimeSet.class);
        recordMap.put((byte) 0x19, LowBattery.class);
        recordMap.put((byte) 0x1A, BatteryActivity.class);
        recordMap.put((byte) 0x1E, PumpSuspended.class);
        recordMap.put((byte) 0x1F, PumpResumed.class);
        recordMap.put((byte) 0x21, Rewound.class);
        recordMap.put((byte) 0x26, ToggleRemote.class);
        recordMap.put((byte) 0x27, ChangeRemoteId.class);
        recordMap.put((byte) 0x33, TempBasalRate.class);
        recordMap.put((byte) 0x34, LowReservoir.class);
        recordMap.put((byte) 0x3f, Ian3F.class);
        recordMap.put((byte) 0x5a, BolusWizardChange.class);
        recordMap.put((byte) 0x5b, BolusWizard.class);
        recordMap.put((byte) 0x5c, UnabsorbedInsulin.class);
        recordMap.put((byte) 0x6c, Old6c.class);
        recordMap.put((byte) 0x6d, ResultTotals.class);
        recordMap.put((byte) 0x6e, Sara6E.class);
        recordMap.put((byte) 0x63, ChangeUtility.class);
        recordMap.put((byte) 0x64, ChangeTimeDisplay.class);
        recordMap.put((byte) 0x7b, BasalProfileStart.class);
 */


/*
From original run-history example:
First command (tell pump to send history)
01 00 a7 01 46 73 24 80 01 00 02 02 00 80 f6 00 00

01 00 a7 01 (Send command to pump)
46 73 24 (pump serial)
80 01 (one parameter)
00 (button)
02 (two retries)
02 (two records expected/requested)
00 (unknown zero)
80 (read history command code)
f6 (checksum for packet so far)
00 (page number?)
00 (checksum for parameters)

pump responded with 64 bytes.

We sent:
01 00 A7 01 46 73 24 80 01 00 02 02 00 80 F6 00 00

same, but we got zero-byte response from pump.  Why?
>> because we took too long to read the response from the pump!

Here is a history response, in raw form:
    0x00000000 6D 46 8F 05 0C 00 E8 00 00 00 00 00 3A 00 3A 64 mF..........:.:d
    0x00000010 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000020 00 00 00 00 00 00 0C 00 E8 00 00 00 3C 00 4D 6F ............<.Mo
    0x00000030 0B 07 0F 3D 75 16 17 A7 8D CF 3E C0 0C 80 00 00 ...=u.....>.....
    0x00000040 00 26 01 42 70 0B 07 0F 27 00 00 00 00 00 00 28 .&.Bp...'......(
    0x00000050 00 00 00 00 00 00 26 00 45 70 0B 07 0F 27 00 00 ......&.Ep...'..
    0x00000060 00 00 00 00 28 00 00 00 00 00 00 1B 03 65 70 0B ....(........ep.
    0x00000070 07 0F 1B 07 4D 71 0B 07 0F 0A 69 4E 4C 2C 07 0F ....Mq....iNL,..
    0x00000080 5B 69 5F 4C 0C 07 0F 17 50 09 32 64 00 19 00 00 [i_L....P.2d....
    0x00000090 00 00 19 6E 01 19 19 00 5F 4C 4C 07 0F 06 06 09 ...n...._LL.....
    0x000000A0 B3 40 4C 53 A7 0F 0C 06 5A 4E 13 07 0F 07 00 00 .@LS....ZN......
    0x000000B0 00 9E 47 00 00 00 00 00 00 00 00 00 00 00 00 00 ..G.............
    0x000000C0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000D0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000F0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000100 8F 05 0C 00 E8 00 00 00 00 00 38 00 38 64 00 00 ..........8.8d..
    0x00000110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000120 00 00 00 00 0C 00 E8 00 00 00 07 00 00 00 3A 49 ..............:I
    0x00000130 8F 6D 49 8F 05 0C 00 E8 00 00 00 00 00 3A 00 3A .mI..........:.:
    0x00000140 64 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 d...............
    0x00000150 00 00 00 00 00 00 00 0C 00 E8 00 00 00 07 00 00 ................
    0x00000160 00 3A 4A 8F 6D 4A 8F 05 0C 00 E8 00 00 00 00 00 .:J.mJ..........
    0x00000170 3A 00 3A 64 00 00 00 00 00 00 00 00 00 00 00 00 :.:d............
    0x00000180 00 00 00 00 00 00 00 00 00 00 0C 00 E8 00 00 00 ................
    0x00000190 07 00 00 00 3A 4B 8F 6D 4B 8F 05 0C 00 E8 00 00 ....:K.mK.......
    0x000001A0 00 00 00 3A 00 3A 64 00 00 00 00 00 00 00 00 00 ...:.:d.........
    0x000001B0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001C0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001D0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001F0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000200 57 0A 0D 0F 0F 50 09 32 64 00 10 00 00 00 00 10 W....P.2d.......
    0x00000210 6E 01 10 10 00 54 57 4A 0D 0F 00 00 00 00 00 00 n....TWJ........
    0x00000220 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000230 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000240 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000250 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000260 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000270 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000280 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000290 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002A0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002B0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002C0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002D0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002F0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000300 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000310 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000320 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000330 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000340 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000350 00 00 00

To parse: parse a record, use the first byte to determine what kind of history event it describes.

Here is a recent failed attempt at history:

    0x00000000 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000010 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000030 00 00 00 00 00 00 00 00 00 00 00 00 00 00 38 01 ..............8.
    0x00000040 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000050 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000060 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000070 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000080 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000090 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000A0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000B0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000C0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000D0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000000F0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000100 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000120 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000130 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000140 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000150 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000160 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000170 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000180 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000190 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001A0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001B0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001C0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001D0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000001F0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000200 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000210 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000220 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000230 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000240 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000250 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000260 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000270 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000280 1B 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000290 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002A0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002B0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x000002C0 00 00 00 6E 00 3A 35 00 34 2F 00 0C 00 34 2F 00 ...n.:5.4/...4/.
    0x000002D0 34 64 00 00 00 00 00 00 01 01 00 00 00 0C 00 E8 4d..............
    0x000002E0 00 00 00 1A 00 51 49 0B 1B 0F 1A 01 68 49 0B 1B .....QI.....hI..
    0x000002F0 0F 21 00 6D 4D 0B 1B 0F 03 00 00 00 6E 5F 4E 2B .!.mM.......n_N+
    0x00000300 00 E8 00 00 00 0A 6A 58 7A 35 1A 0F 5B 6A 61 7A ......jXz5..[jaz
    0x00000310 15 1A 0F 0C 50 09 32 64 00 0D 00 00 00 00 0D 6E ....P.2d.......n
    0x00000320 01 0D 0D 00 61 7A 55 1A 0F 19 00 40 41 16 1A 0F ....azU....@A...
    0x00000330 07 00 00 00 6E 5A 8F 6D 5A 8F 05 00 6A 6A 6A 01 ....nZ.mZ...jjj.
    0x00000340 6D 56 8F 05 0C 00 E8 00 00 00 00 00 3A 00 3A 64 mV..........:.:d
    0x00000350 00 00 00

 */
