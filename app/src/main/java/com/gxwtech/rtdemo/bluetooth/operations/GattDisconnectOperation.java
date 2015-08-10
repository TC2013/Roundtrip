package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;

public class GattDisconnectOperation extends GattOperation {

    public GattDisconnectOperation() {
        super();
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        gatt.disconnect();
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }
}
