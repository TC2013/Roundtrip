package com.roundtrip.decoding.packages;

import android.support.annotation.NonNull;

import com.roundtrip.bluetooth.CRC;
import com.roundtrip.decoding.GlucosUnitConversion;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.TimeZone;

public class MeterReading extends MedtronicReading implements Comparator<MeterReading>, Comparable<MeterReading> {
    private static final String TAG = "MeterReading";

    private int mgdl = -1;

    public MeterReading(byte[] readData) throws ParseException {
        super(readData);

        if (readData.length != packageLength()) {
            throw new InvalidLengthException("Invalid message length");
        }

        byte crcComputed = CRC.computeCRC8(readData, 2, 8);
        if (crcComputed != readData[readData.length - 1]) {
            throw new InvalidCRCException("Invalid message CRC");
        }

        mgdl = ByteBuffer.wrap(new byte[]{0x00, 0x00, readData[6], readData[7]}).getInt();

        if(mgdl > 1000) {
            throw new InvalidDataException("Invalid message CRC");
        }
    }

    public int getMgdl() {
        return this.mgdl;
    }

    @Override
    protected int packageLength() {
        return 9;
    }

    @Override
    public int compareTo(@NonNull MeterReading meterReading) {
        return this.compare(this, meterReading);
    }

    @Override
    public boolean equals(@NonNull Object medtronicGlucose) {
        return medtronicGlucose instanceof MeterReading && this.compareTo((MeterReading) medtronicGlucose) == 0;
    }

    @Override
    public int compare(MeterReading glucoseOne, MeterReading glucoseTwo) {
        Long t1 = glucoseOne.getCreated().getTime();
        Long t2 = glucoseTwo.getCreated().getTime();

        // If they are within four minutes, and the same value, they must be the same :)
        if(Math.abs(t1 - t2) < EXPIRATION_FOUR_MINUTES && glucoseOne.getMgdl() == glucoseTwo.getMgdl()) {
            return 0;
        }

        return t1.compareTo(t2);
    }

    @Override
    public String toString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        return "Glucose reading " + mgdl + "mg/dl " + GlucosUnitConversion.mgdlToMmol(mgdl) + " mmol, at " + df.format(this.created);
    }
}
