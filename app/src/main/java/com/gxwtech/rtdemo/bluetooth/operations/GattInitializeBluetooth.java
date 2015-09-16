package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;

/**
 * Created by Fokko on 11-8-15.
 */
public class GattInitializeBluetooth extends GattOperation {
    @Override
    public void execute(BluetoothGatt bluetoothGatt) {
        // Do nothing, just a stub :)
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return false;
    }

    @Override
    public String toString() {
        return "GattInitializeBluetooth";
    }
}
