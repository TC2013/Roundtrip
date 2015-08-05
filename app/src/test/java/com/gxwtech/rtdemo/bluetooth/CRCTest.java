package com.gxwtech.rtdemo.bluetooth;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by Fokko on 5-8-15.
 */
public class CRCTest extends TestCase {

    // Based on: https://github.com/bewest/decoding-carelink/blob/master/decocare/lib.py

    @Test
    public void testComputeCRC() throws Exception {
        byte[] input = new byte[]{0x00, (byte) 0xFF, 0x00};

        assertEquals(CRC.computeCRC(input), (byte) 0xB1);
    }

    @Test
    public void testAppendCRC() throws Exception {
        byte[] input = new byte[]{0x00, (byte) 0xFF, 0x00};

        byte[] output = CRC.appendCRC(input);

        assertEquals(CRC.computeCRC(input), output[input.length]);
    }
}
