package com.gxwtech.rtdemo.medtronic.PumpData.records;

import android.util.Log;

import com.gxwtech.rtdemo.medtronic.PumpModel;

abstract public class Record {
    private static final String TAG = "Record";
    protected byte recordOp;
    protected int totalSize = -1;
    protected byte timestampSize = 0; // not all records have time stamps
    protected int bodySize = 0; // should be overridden in derived.
    protected byte headerSize = 2; // minimum?
    // first byte is record type, second byte (of header) is often the only parameter
    // sometimes followed by a date (see TimeStampedRecord), which is sometimes followed by more data
    protected PumpModel model = PumpModel.UNSET;
    protected String recordTypeName = this.getClass().getSimpleName();

    public Record() {
    }

    public boolean collectRawData(byte[] data, PumpModel model) {
        recordOp = data[0];
        this.model = model;
        return true;
    }

    // size is invalid (-1) for some classes, until they
    // have read their data, as they are variable length
    public int getSize() {
        return totalSize;
    }

    // When a class can calculate its size, do so.
    // For some, this is a constant,
    // for others, it can be done when the model is known
    // for still others, the entire record must be parsed.
    protected void calcSize() {
        totalSize = headerSize + timestampSize + bodySize;
    }

    public byte getRecordOp() {
        return recordOp;
    }

    public void logRecord() {
        Log.i(TAG, String.format("Unparsed %s, size = %d",recordTypeName,getSize()));
    }

    protected boolean decode(byte[] data) {
        return true;
    }
    protected static int readUnsignedByte(byte b) { return (b<0)?b+256:b;}
}
