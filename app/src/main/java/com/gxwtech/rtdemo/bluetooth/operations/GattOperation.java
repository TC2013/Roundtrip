package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;

public abstract class GattOperation {
    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 10000;

    public GattOperation() {

    }

    public abstract void execute(final BluetoothGatt bluetoothGatt);

    public abstract boolean hasAvailableCompletionCallback();
}
