package com.roundtrip.bluetooth;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class RileyLinkUtilTest {

    @Test
    public void testComputeNewSize() throws Exception {
        Assert.assertEquals(2, RileyLinkUtil.computeNewSize(1));
        Assert.assertEquals(3, RileyLinkUtil.computeNewSize(2));
        Assert.assertEquals(5, RileyLinkUtil.computeNewSize(3));
        Assert.assertEquals(6, RileyLinkUtil.computeNewSize(4));
        Assert.assertEquals(8, RileyLinkUtil.computeNewSize(5));
    }

    @Test
    public void testComposeRFBytes() throws Exception {

        byte[] testSingle = new byte[]{(byte) 0xa7};
        byte[] testSingleOutput = new byte[]{(byte) 0xa9, 0x60};

        assertArrayEquals(testSingleOutput, RileyLinkUtil.composeRFStream(testSingle));

        byte[] testDouble = new byte[]{(byte) 0xa7, 0x12};
        byte[] testDoubleOutput = new byte[]{(byte) 0xa9, 0x6c, 0x72};

        assertArrayEquals(testDoubleOutput, RileyLinkUtil.composeRFStream(testDouble));

        byte[] testTriple = new byte[]{(byte) 0xa7, 0x12, (byte) 0xa7};
        byte[] testTripleOutput = new byte[]{(byte) 0xa9, 0x6c, 0x72, (byte) 0xa9, 0x60};

        assertArrayEquals(testTripleOutput, RileyLinkUtil.composeRFStream(testTriple));
    }

}