package com.gxwtech.rtdemo.decoding;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Created by fokko on 7-8-15.
 */
public class MedtronicGlucoseTest extends TestCase {

    @Test
    public void testDecode() throws Exception {
        MedtronicGlucose gluc = new MedtronicGlucose();

        gluc.decode(new byte[] {
                (byte) 0xF6,
                (byte) 0x07,
                (byte) 0xA5,
                (byte) 0xC0,
                (byte) 0xB1,
                (byte) 0xFA,
                (byte) 0x01,
                (byte) 0x38,
                (byte) 0xBD
        });

        assertEquals(gluc.getMgdl(), 312);
    }
}