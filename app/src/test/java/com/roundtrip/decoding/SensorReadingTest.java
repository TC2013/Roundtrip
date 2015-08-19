package com.roundtrip.decoding;

import com.roundtrip.decoding.packages.InvalidCRCException;
import com.roundtrip.decoding.packages.InvalidLengthException;
import com.roundtrip.decoding.packages.SensorMeasurement;
import com.roundtrip.decoding.packages.SensorReading;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Iterator;

public class SensorReadingTest {

    @Test
    public void testDecode() throws Exception {

        SensorReading gluc = new SensorReading(new byte[]{
                (byte) 0xE9,
                (byte) 0x00,
                (byte) 0xAB,
                (byte) 0x0F,
                (byte) 0x26,
                (byte) 0xC8,
                (byte) 0x8F,
                (byte) 0x0D,
                (byte) 0x0B,
                (byte) 0x12,
                (byte) 0x40,
                (byte) 0x13,
                (byte) 0xC1,
                (byte) 0x14,
                (byte) 0x05,
                (byte) 0x00,
                (byte) 0x4B,
                (byte) 0x4B,
                (byte) 0xA3,
                (byte) 0x14,
                (byte) 0x0B,
                (byte) 0x14,
                (byte) 0x16,
                (byte) 0x14,
                (byte) 0x56,
                (byte) 0x14,
                (byte) 0xCB,
                (byte) 0x14,
                (byte) 0xFB,
                (byte) 0x15,
                (byte) 0x0C,
                (byte) 0x15,
                (byte) 0x1C,
                (byte) 0x00,
                (byte) 0xF2,
                (byte) 0xB6
        });

    }


    public byte[][] sequences = new byte[][]{
            //                  0           1           2           3           4           5               6           7           8           9               10          11          12          13              14          15          16          17              18          19          20          21              22          23          24          25            26            27          28          29              30          31          32          33
            new byte[]{(byte) 0xE2, (byte) 0xAF, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x30, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x00, (byte) 0x4D, (byte) 0x4D, (byte) 0xA3, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x15, (byte) 0xC8, (byte) 0x17, (byte) 0xA5, (byte) 0x00, (byte) 0x59, (byte) 0xFC},
            new byte[]{(byte) 0xE5, (byte) 0xB0, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x31, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x00, (byte) 0x4D, (byte) 0x4D, (byte) 0xA3, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x15, (byte) 0xC8, (byte) 0x17, (byte) 0xA5, (byte) 0x00, (byte) 0x6E, (byte) 0xFF},
            new byte[]{(byte) 0xF0, (byte) 0xB1, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x40, (byte) 0x15, (byte) 0x7D, (byte) 0x14, (byte) 0x92, (byte) 0x00, (byte) 0x4C, (byte) 0x4D, (byte) 0xA3, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x15, (byte) 0xC8, (byte) 0x00, (byte) 0x8F, (byte) 0x9F},
            new byte[]{(byte) 0xF4, (byte) 0xB2, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x41, (byte) 0x15, (byte) 0x7D, (byte) 0x14, (byte) 0x92, (byte) 0x00, (byte) 0x4C, (byte) 0x4D, (byte) 0xA3, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x15, (byte) 0xC8, (byte) 0x00, (byte) 0xB8, (byte) 0x9C},
            new byte[]{(byte) 0xF5, (byte) 0xB3, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x50, (byte) 0x17, (byte) 0x70, (byte) 0x15, (byte) 0x7D, (byte) 0x00, (byte) 0x4B, (byte) 0x4C, (byte) 0xA3, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x00, (byte) 0x26, (byte) 0x60},
            new byte[]{(byte) 0xF6, (byte) 0xB4, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x51, (byte) 0x17, (byte) 0x70, (byte) 0x15, (byte) 0x7D, (byte) 0x00, (byte) 0x4B, (byte) 0x4C, (byte) 0xA3, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x00, (byte) 0x11, (byte) 0x63}
    };


    @Test
    public void testNumberOfMeasurements() throws Exception {
        Assert.assertEquals(8, new SensorReading(sequences[0]).getIsigMeasurements().size());
    }

    @Test
    public void testSequence() throws Exception {

        // Checking if all the subsequent messages are processed properly.

        // Without repeated messages
        byte[][] uniqueSequences = new byte[][]{
                //                  0           1           2           3           4           5               6           7           8           9               10          11          12          13              14          15          16          17              18          19          20          21              22          23          24          25            26            27          28          29              30          31          32          33
                new byte[]{(byte) 0xF5, (byte) 0xB3, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x50, (byte) 0x17, (byte) 0x70, (byte) 0x15, (byte) 0x7D, (byte) 0x00, (byte) 0x4B, (byte) 0x4C, (byte) 0xA3, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x00, (byte) 0x26, (byte) 0x60},
                new byte[]{(byte) 0xF0, (byte) 0xB1, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x40, (byte) 0x15, (byte) 0x7D, (byte) 0x14, (byte) 0x92, (byte) 0x00, (byte) 0x4C, (byte) 0x4D, (byte) 0xA3, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x15, (byte) 0xC8, (byte) 0x00, (byte) 0x8F, (byte) 0x9F},
                new byte[]{(byte) 0xE2, (byte) 0xAF, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x30, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x00, (byte) 0x4D, (byte) 0x4D, (byte) 0xA3, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x15, (byte) 0xC8, (byte) 0x17, (byte) 0xA5, (byte) 0x00, (byte) 0x59, (byte) 0xFC},
        };

        SensorReading[] readings = new SensorReading[uniqueSequences.length];

        int pos = 0;
        for (byte[] sequence : uniqueSequences) {
            readings[pos++] = new SensorReading(sequence);
        }

        Iterator<SensorMeasurement> measurementsNewest = readings[0].getIsigMeasurements().iterator();
        measurementsNewest.next();

        Iterator<SensorMeasurement> measurementOneBefore = readings[1].getIsigMeasurements().iterator();
        if (measurementOneBefore.hasNext() && measurementsNewest.hasNext()) {
            Assert.assertEquals(measurementsNewest.next().getIsig(), measurementOneBefore.next().getIsig());
        }

        measurementsNewest = readings[0].getIsigMeasurements().iterator();
        measurementsNewest.next();
        measurementsNewest.next();

        Iterator<SensorMeasurement> measurementTwoBefore = readings[2].getIsigMeasurements().iterator();
        if (measurementTwoBefore.hasNext() && measurementsNewest.hasNext()) {
            Assert.assertEquals(measurementsNewest.next().getIsig(), measurementTwoBefore.next().getIsig());
        }

    }

    @Test(expected = InvalidCRCException.class)
    public void testCRC() {
        new SensorReading(new byte[]{(byte) 0xF6, (byte) 0xB4, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x51, (byte) 0x17, (byte) 0x70, (byte) 0x15, (byte) 0x7D, (byte) 0x00, (byte) 0x4B, (byte) 0x4C, (byte) 0xA3, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x00, (byte) 0x11, (byte) 0x64});
    }
/*
    @Test(expected = InvalidSerialException.class)
    public void testSerial() {
        new SensorReading(new byte[]{(byte) 0xF6, (byte) 0xB4, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xA8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x51, (byte) 0x17, (byte) 0x70, (byte) 0x15, (byte) 0x7D, (byte) 0x00, (byte) 0x4B, (byte) 0x4C, (byte) 0xA3, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5, (byte) 0x00, (byte) 0x11, (byte) 0x64});
    }*/

    @Test(expected = InvalidLengthException.class)
    public void testLength() {
        new SensorReading(new byte[]{(byte) 0xF6, (byte) 0xB4, (byte) 0xAB, (byte) 0x0F, (byte) 0x26, (byte) 0xC8, (byte) 0x8F, (byte) 0x0D, (byte) 0x0B, (byte) 0x12, (byte) 0x51, (byte) 0x17, (byte) 0x70, (byte) 0x15, (byte) 0x7D, (byte) 0x00, (byte) 0x4B, (byte) 0x4C, (byte) 0xA3, (byte) 0x14, (byte) 0x92, (byte) 0x14, (byte) 0x7F, (byte) 0x14, (byte) 0x81, (byte) 0x14, (byte) 0x3F, (byte) 0x14, (byte) 0x4B, (byte) 0x14, (byte) 0xDF, (byte) 0x14, (byte) 0xE5});
    }
}