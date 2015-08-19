package com.roundtrip.decoding;

import android.util.Log;

import com.roundtrip.decoding.packages.MedtronicReading;
import com.roundtrip.decoding.packages.MeterReading;
import com.roundtrip.decoding.packages.ParseException;
import com.roundtrip.decoding.packages.SensorReading;
import com.roundtrip.decoding.packages.SensorWarmupReading;
import com.roundtrip.medtronic.MedtronicConstants;
import com.roundtrip.processing.MedtronicProcessor;

public class MedtronicDecoder {
    private static final String TAG = "MedtronicDecoder";

    public static MedtronicReading DeterminePackage(final byte[] data) {
        final MedtronicReading newDataPackage;
        try {
            if (data.length > 2) {
                switch (data[2]) {
                    case MedtronicConstants.MEDTRONIC_GLUCOSE:
                        Log.d(TAG, "Found a sensor package");

                        newDataPackage = new MeterReading(data);
                        break;
                    case MedtronicConstants.MEDTRONIC_SENSOR:
                        Log.d(TAG, "Found a sensor package");

                        newDataPackage = new SensorReading(data);
                        break;
                    case MedtronicConstants.MEDTRONIC_SENSOR_WARMUP:
                        Log.d(TAG, "Found a sensor package, but is still warming up");

                        newDataPackage = new SensorWarmupReading(data);
                        break;
                    default:
                        Log.d(TAG, "Found an unknown package");

                        newDataPackage = null;
                }
            } else {
                Log.d(TAG, "Found gibberish");
                newDataPackage = null;
            }
        } catch (ParseException e) {
            return null;
        }

        if (newDataPackage != null) {
            Log.d(TAG, "Processing message: " + newDataPackage);
            MedtronicProcessor.getInstance().process(newDataPackage);
        }

        return newDataPackage;
    }
}
