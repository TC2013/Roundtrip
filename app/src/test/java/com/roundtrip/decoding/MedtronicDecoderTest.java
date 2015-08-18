package com.roundtrip.decoding;

import com.roundtrip.decoding.packages.MeterReading;

import org.junit.Assert;
import org.junit.Test;

public class MedtronicDecoderTest {

    @Test
    public void testDeterminePackage() throws Exception {
        Assert.assertEquals(MedtronicDecoder.DeterminePackage(new byte[]{0x00, 0x00, (byte) 0xa5, 0x00, 0x00, 0x00, 0x00, 0x00, 40}).getClass(), MeterReading.class);
    }
}