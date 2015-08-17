package com.gxwtech.rtdemo.decoding;

import com.gxwtech.rtdemo.decoding.packages.MedtronicPackage;
import com.gxwtech.rtdemo.decoding.packages.MedtronicGlucose;
import com.gxwtech.rtdemo.decoding.packages.MedtronicSensor;
import com.gxwtech.rtdemo.medtronic.MedtronicConstants;

/**
 * Created by fokko on 6-8-15.
 */
public class Decoder {
    public static MedtronicPackage DeterminePackage(final byte[] data) {
        final MedtronicPackage newDataPackage;
        if(data.length > 2) {
            switch (data[2]) {
                case MedtronicConstants.MEDTRONIC_GLUCOSE:
                    if(data.length == 36) {
                        newDataPackage = new MedtronicGlucose();
                        newDataPackage.decode(data);
                    } else {
                        newDataPackage = null;
                    }
                    break;
                case MedtronicConstants.MEDTRONIC_SENSOR:
                    newDataPackage = new MedtronicSensor();
                    newDataPackage.decode(data);
                    break;
                default:
                    newDataPackage = null;
            }
        }else {
            newDataPackage = null;
        }
        return newDataPackage;
    }
}
