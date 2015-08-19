package com.roundtrip.enlite.calibration;

import junit.framework.Assert;

import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class TwoPointCalibrationTest {

    @Test
    public void testApproximateGlucoseLevel() throws Exception {

        TwoPointCalibration call = new TwoPointCalibration();

        LinkedList<CalibrationPair> calList = new LinkedList<>();
        calList.add(new CalibrationPair(48,8));
        calList.add(new CalibrationPair(24,4));

        double res = call.approximateGlucoseLevel(36, calList);

        Assert.assertEquals(res, 6.0);
    }
}