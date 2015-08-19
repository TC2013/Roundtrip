package com.roundtrip.enlite.calibration;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import java.util.LinkedList;

public class LinearRegressionCalibrationTest {

    @Test
    public void testApproximateGlucoseLevel() throws Exception {

        LinearRegressionCalibration call = new LinearRegressionCalibration();

        LinkedList<CalibrationPair> calList = new LinkedList<>();
        calList.add(new CalibrationPair(48,8));
        calList.add(new CalibrationPair(24,4));
        calList.add(new CalibrationPair(60,10));

        double res = call.approximateGlucoseLevel(36, calList);

        Assert.assertEquals(6.0,res);
    }
}