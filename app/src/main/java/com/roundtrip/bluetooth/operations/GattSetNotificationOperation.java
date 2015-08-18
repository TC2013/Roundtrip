package com.roundtrip.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.roundtrip.bluetooth.GattAttributes;

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

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(mDescriptorUuid);

        if (0 != (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
            // It's an indicate characteristic
            Log.d(TAG, "Characteristic " + GattAttributes.lookup(characteristic.getUuid()) + ", descriptor: " + GattAttributes.lookup(mDescriptorUuid) + " is INDICATE");
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

            if (gatt.writeDescriptor(descriptor)) {
                Log.d(TAG, "Succesfully written descriptor");
            } else {
                Log.d(TAG, "Unable to written decriptor");
            }
        }
        else if (0 != (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            // It's a notify characteristic
            Log.d(TAG,  "Characteristic " + GattAttributes.lookup(characteristic.getUuid()) + ", descriptor: " + GattAttributes.lookup(mDescriptorUuid) + " is NOTIFY");
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            if (gatt.writeDescriptor(descriptor)) {
                Log.d(TAG, "Succesfully written descriptor");
            } else {
                Log.d(TAG, "Unable to written decriptor");
            }
        }
    }

    // Callback in the overridden method
    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    @Override
    public String toString() {
        return "GattSetNotificationOperation on service: " + GattAttributes.lookup(mServiceUuid) + ", Char: " + GattAttributes.lookup(mCharacteristicUuid);
    }
}
