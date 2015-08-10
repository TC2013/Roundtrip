package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.gxwtech.rtdemo.bluetooth.GattCharacteristicReadCallback;

import java.util.UUID;

public class GattCharacteristicReadOperation extends GattOperation {
    private static final String TAG = "GattCharacteristicReadOperation";

    private final UUID mService;
    private final UUID mCharacteristic;
    private final GattCharacteristicReadCallback mCallback;

    public GattCharacteristicReadOperation(final UUID service, final UUID characteristic,final  GattCharacteristicReadCallback callback) {
        super();
        mService = service;
        mCharacteristic = characteristic;
        mCallback = callback;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = gatt.getService(mService).getCharacteristic(mCharacteristic);
        gatt.readCharacteristic(characteristic);
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    public void onRead(final BluetoothGattCharacteristic characteristic) {
        if (mCallback != null) {
            mCallback.call(characteristic.getValue());
        }
    }
}
