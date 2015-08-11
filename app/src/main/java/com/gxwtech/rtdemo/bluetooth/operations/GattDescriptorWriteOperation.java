package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.gxwtech.rtdemo.bluetooth.GattAttributes;

import java.util.UUID;

public class GattDescriptorWriteOperation extends GattOperation {
    private static final String TAG = "GattDescriptorWriteOperation";

    private final UUID mService;
    private final UUID mCharacteristic;
    private final UUID mDescriptor;

    public GattDescriptorWriteOperation(final UUID service,
                                        final UUID characteristic,
                                        final UUID descriptor) {
        super();
        mService = service;
        mCharacteristic = characteristic;
        mDescriptor = descriptor;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        Log.d(TAG, "Writing to " + mDescriptor);
        BluetoothGattDescriptor descriptor = gatt.getService(mService).getCharacteristic(mCharacteristic).getDescriptor(mDescriptor);
        gatt.writeDescriptor(descriptor);
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    @Override
    public String toString() {
        return "GattDescriptorWriteOperation on service: " + GattAttributes.lookup(mService) + ", Char: " + GattAttributes.lookup(mCharacteristic);
    }
}
