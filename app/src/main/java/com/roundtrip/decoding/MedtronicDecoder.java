package com.roundtrip.decoding;

import com.roundtrip.decoding.packages.MedtronicReading;
import com.roundtrip.decoding.packages.MeterReading;
import com.roundtrip.decoding.packages.ParseException;
import com.roundtrip.decoding.packages.SensorReading;
import com.roundtrip.decoding.packages.SensorWarmupReading;
import com.roundtrip.medtronic.MedtronicConstants;

public class MedtronicDecoder {
    public static MedtronicReading DeterminePackage(final byte[] data) {
        final MedtronicReading newDataPackage;
        try {
            if (data.length > 2) {
                switch (data[2]) {
                    case MedtronicConstants.MEDTRONIC_GLUCOSE:
                        newDataPackage = new MeterReading(data);
                        break;
                    case MedtronicConstants.MEDTRONIC_SENSOR:
                        newDataPackage = new SensorReading(data);
                        break;
                    case MedtronicConstants.MEDTRONIC_SENSOR_WARMUP:
                        newDataPackage = new SensorWarmupReading(data);
                        break;
                    default:
                        newDataPackage = null;
                }
            } else {
                newDataPackage = null;
            }
        } catch (ParseException e) {
            return null;
        }

        return newDataPackage;
    }
}
