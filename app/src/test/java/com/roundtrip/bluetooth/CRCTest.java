package com.roundtrip.bluetooth;

import org.junit.Assert;
import org.junit.Test;

public class CRCTest {

    // Based on: https://github.com/bewest/decoding-carelink/blob/master/decocare/lib.py

    @Test
    public void testComputeCRC() throws Exception {
        byte[] input = new byte[]{0x00, (byte) 0xFF, 0x00};

        Assert.assertEquals(CRC.computeCRC8(input), (byte) 0xB1); // 0xB1 == 177
    }

    @Test
    public void testAppendCRC() throws Exception {
        byte[] input = new byte[]{0x00, (byte) 0xFF, 0x00};

        byte[] output = CRC.appendCRC(input);

        Assert.assertEquals(CRC.computeCRC8(input), output[input.length]);
    }
}
