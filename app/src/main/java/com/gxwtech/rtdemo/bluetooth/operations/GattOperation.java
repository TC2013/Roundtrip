package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;

public abstract class GattOperation {

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 10000;
    private GattOperation mOperation;

    public GattOperation() {

    }

    public abstract void execute(final BluetoothGatt bluetoothGatt);

    public int getTimoutInMillis() {
        return DEFAULT_TIMEOUT_IN_MILLIS;
    }

    public abstract boolean hasAvailableCompletionCallback();
}
