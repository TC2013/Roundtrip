package com.roundtrip.enlite.calibration;

import junit.framework.Assert;

import org.junit.Test;

import java.util.LinkedList;

public class OnePointCalibrationTest {

    @Test
    public void testApproximateGlucoseLevel() throws Exception {

        OnePointCalibration call = new OnePointCalibration();

        LinkedList<CalibrationPair> calList = new LinkedList<>();
        calList.add(new CalibrationPair(36, 6));

        double res = call.approximateGlucoseLevel(24, calList);

        Assert.assertEquals(4.0, res);
    }
}