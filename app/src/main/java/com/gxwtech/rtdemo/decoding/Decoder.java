package com.gxwtech.rtdemo.decoding;

import com.gxwtech.rtdemo.medtronic.MedtronicConstants;

/**
 * Created by fokko on 6-8-15.
 */
public class Decoder {
    public static DataPackage DeterminePackage(byte[] data) {
        final DataPackage newDataPackage;
        switch (data[2]) {
            case MedtronicConstants.MEDTRONIC_GLUCOSE:
                newDataPackage = new MedtronicGlucose();
                newDataPackage.decode(data);
                break;
            default:
                newDataPackage = null;
        }
        return newDataPackage;
    }
}
