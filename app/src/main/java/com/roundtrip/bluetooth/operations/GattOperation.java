package com.roundtrip.bluetooth.operations;

import android.bluetooth.BluetoothGatt;

public abstract class GattOperation {
    public GattOperation() {

    }

    public abstract void execute(final BluetoothGatt bluetoothGatt);

    public abstract boolean hasAvailableCompletionCallback();
}
