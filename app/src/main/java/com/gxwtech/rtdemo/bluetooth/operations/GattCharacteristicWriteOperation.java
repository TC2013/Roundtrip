package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.CRC;
import com.gxwtech.rtdemo.bluetooth.GattAttributes;
import com.gxwtech.rtdemo.bluetooth.RileyLinkUtil;

import java.util.UUID;

public class GattCharacteristicWriteOperation extends GattOperation {
    private static final String TAG = "GattCharacteristicWriteOperation";

    private final UUID mService;
    private final UUID mCharacteristic;
    private final byte[] mValue;

    public GattCharacteristicWriteOperation(final UUID service, final UUID characteristic, byte[] value, final boolean addCRC, final boolean transform) {
        super();
        mService = service;
        mCharacteristic = characteristic;

        if (addCRC) {
            value = CRC.appendCRC(value);
            Log.d(TAG, "CRC: " + BluetoothConnection.toHexString(value));
        }

        if (transform) {
            value = RileyLinkUtil.composeRFStream(value);
            Log.d(TAG, "Transformed: " + BluetoothConnection.toHexString(value));
        }

        mValue = value;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = gatt.getService(mService).getCharacteristic(mCharacteristic);
        characteristic.setValue(mValue);
        gatt.writeCharacteristic(characteristic);
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    @Override
    public String toString() {
        return "GattCharacteristicWriteOperation on service: " + GattAttributes.lookup(mService) + ", Char: " + GattAttributes.lookup(mCharacteristic);
    }
}
