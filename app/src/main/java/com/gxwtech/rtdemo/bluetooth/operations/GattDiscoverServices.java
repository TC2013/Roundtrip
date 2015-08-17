package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

public class GattDiscoverServices extends GattOperation {
    private static final String TAG = "DiscoverServices";

    @Override
    public void execute(BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt.discoverServices()) {
            Log.w(TAG, "Starting to discover GATT Services.");

        } else {
            Log.w(TAG, "Cannot discover GATT Services.");
        }
    }

    // Special callback which only is invoked after the services are returned
    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    @Override
    public String toString() {
        return "GattDiscoverServices";
    }
}
