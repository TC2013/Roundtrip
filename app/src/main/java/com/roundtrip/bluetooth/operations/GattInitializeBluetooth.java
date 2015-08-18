package com.roundtrip.bluetooth.operations;

import android.bluetooth.BluetoothGatt;

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
