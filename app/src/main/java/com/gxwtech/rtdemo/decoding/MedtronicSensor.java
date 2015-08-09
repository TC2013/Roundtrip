package com.gxwtech.rtdemo.decoding;

import android.util.Log;

import com.gxwtech.rtdemo.bluetooth.CRC;

/**
 * Created by Fokko on 7-8-15.
 */
public class MedtronicSensor extends DataPackage {
    private static final String TAG = "MedtronicMensor";

    @Override
    public void decode(final byte[] readData) {

        //TODO 5-7 Enlite ID, check this
        if(readData.length != packageLength()) {
            Log.w(TAG, "Unknown length of data.");
            return;
        }

        byte[] serial = toBytes(2541711);

        if(serial[0] != readData[4] ||
            serial[1] != readData[5] ||
            serial[2] != readData[6]) {
            Log.w(TAG, "Found package with invalid serial.");
            return;
        }

        /*
        byte crcComputed = CRC.computeCRC(readData, 1, 34);
        if(crcComputed != readData[readData.length-1]) {
            System.out.println(readData[readData.length-1]);
            System.out.println(crcComputed);
            Log.w(TAG, "Invalid CRC.");
            return;
        }*/




    }

    private byte[] toBytes(int i) {
        byte[] result = new byte[3];

        result[0] = (byte) (i >> 16);
        result[1] = (byte) (i >> 8);
        result[2] = (byte) i;

        return result;
    }

    @Override
    protected int packageLength() {
        return 36;
    }
}
