package com.roundtrip.enlite.calibration;

import com.roundtrip.bluetooth.CRC;
import com.roundtrip.decoding.packages.MeterReading;

import org.junit.Test;

import static org.junit.Assert.*;

public class OnePointCalibrationTest {

    private CalibrationPair createCalibrationPair(int mgdl, int isig) {

        byte[] meterBytes = new byte[]{
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0xA5,
                (byte) 0xC0,
                (byte) 0xB1,
                (byte) 0xFA,
                (byte) (mgdl >> 8 & 0xFF),
                (byte) (mgdl & 0xFF),
                (byte) 0x00
        };

        meterBytes[meterBytes.length-1] = CRC.computeCRC8(meterBytes, 1, 7);

        MeterReading meterReading = new MeterReading(meterBytes);







        return null;
    }

    @Test
    public void testApproximateGlucoseLevel() throws Exception {

    }
}