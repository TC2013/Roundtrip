package com.gxwtech.rtdemo.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.gxwtech.rtdemo.bluetooth.GattAttributes;

import java.util.UUID;

public class GattSetNotificationOperation extends GattOperation {
    private static final String TAG = "GattSetNotificationOperation";

    private final UUID mServiceUuid;
    private final UUID mCharacteristicUuid;
    private final UUID mDescriptorUuid;

    public GattSetNotificationOperation(final UUID serviceUuid,
                                        final UUID characteristicUuid,
                                        final UUID descriptorUuid) {
        super();

        mServiceUuid = serviceUuid;
        mCharacteristicUuid = characteristicUuid;
        mDescriptorUuid = descriptorUuid;
    }

    @Override
    public void execute(final BluetoothGatt gatt) {
        final BluetoothGattCharacteristic characteristic = gatt
                .getService(mServiceUuid)
                .getCharacteristic(mCharacteristicUuid);

        gatt.setCharacteristicNotification(characteristic, true);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(mDescriptorUuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (gatt.writeDescriptor(descriptor)) {
            Log.v(TAG, "Succesfully written descriptor");
        } else {
            Log.v(TAG, "Unable to written decriptor");
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return false;
    }

    @Override
    public String toString() {
        return "GattSetNotificationOperation on service: " + GattAttributes.lookup(mServiceUuid) + ", Char: " + GattAttributes.lookup(mCharacteristicUuid);
    }
}
