package com.roundtrip.decoding.packages;

import android.support.annotation.NonNull;

import com.roundtrip.bluetooth.CRC;

import java.util.Comparator;

public class SensorReading extends MedtronicReading implements Comparator<SensorReading>, Comparable<SensorReading> {
    private static final String TAG = "MedtronicMensor";

    private double isig = 0.0;

    public SensorReading(final byte[] readData) throws ParseException{
        super(readData);

        //TODO 5-7 Enlite ID, check this
        if (readData.length != packageLength()) {
            throw new InvalidLengthException("Invalid message length");
        }

        /*
        byte[] serial = convertSerialToBytes(2541711);

        if (serial[0] != readData[4] ||
                serial[1] != readData[5] ||
                serial[2] != readData[6]) {
            throw new InvalidSerialException("Invalid message serial");
        }*/

        byte[] crcComputed = CRC.computeCRC16(readData, 2, 34);

        if(crcComputed[0] != readData[readData.length-2]
           || crcComputed[1] != readData[readData.length-1]) {
            throw new InvalidCRCException("Invalid message CRC");
        }
    }

    public double getIsig() {
        return this.isig;
    }

    private byte[] convertSerialToBytes(int i) {
        byte[] result = new byte[3];

        result[0] = (byte) (i >> 16);
        result[1] = (byte) (i >> 8);
        result[2] = (byte) i;

        return result;
    }

    @Override
    public int compareTo(@NonNull SensorReading sensorReading) {
        return this.compare(this, sensorReading);
    }

    @Override
    public boolean equals(@NonNull Object medtronicSensor) {
        return medtronicSensor instanceof SensorReading && this.compareTo((SensorReading) medtronicSensor) == 0;
    }

    @Override
    public int compare(SensorReading glucoseOne, SensorReading glucoseTwo) {
        Long t1 = glucoseOne.getCreated().getTime();
        Long t2 = glucoseTwo.getCreated().getTime();

        // If they are within four minutes, and the same value, they must be the same :)
        if(Math.abs(t1 - t2) < EXPIRATION_FOUR_MINUTES && glucoseOne.getIsig() == glucoseTwo.getIsig()) {
            return 0;
        }

        return t1.compareTo(t2);
    }

    @Override
    protected int packageLength() {
        return 36;
    }
}
