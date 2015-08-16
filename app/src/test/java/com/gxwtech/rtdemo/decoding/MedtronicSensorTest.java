package com.gxwtech.rtdemo.decoding;

import com.gxwtech.rtdemo.decoding.packages.MedtronicSensor;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Created by fokko on 9-8-15.
 */
public class MedtronicSensorTest extends TestCase {

    @Test
    public void testDecode() throws Exception {

        MedtronicSensor gluc = new MedtronicSensor();

        gluc.decode(new byte[] {
                (byte)0xE9,
                (byte)0x00,
                (byte)0xAB,
                (byte)0x0F,
                (byte)0x26,
                (byte)0xC8,
                (byte)0x8F,
                (byte)0x0D,
                (byte)0x0B,
                (byte)0x12,
                (byte)0x40,
                (byte)0x13,
                (byte)0xC1,
                (byte)0x14,
                (byte)0x05,
                (byte)0x00,
                (byte)0x4B,
                (byte)0x4B,
                (byte)0xA3,
                (byte)0x14,
                (byte)0x0B,
                (byte)0x14,
                (byte)0x16,
                (byte)0x14,
                (byte)0x56,
                (byte)0x14,
                (byte)0xCB,
                (byte)0x14,
                (byte)0xFB,
                (byte)0x15,
                (byte)0x0C,
                (byte)0x15,
                (byte)0x1C,
                (byte)0x00,
                (byte)0xF2,
                (byte)0xB6
        });

    }
}