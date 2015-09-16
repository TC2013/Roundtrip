package no.nordicsemi.puckcentral.bluetooth.gatt.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import org.droidparts.util.L;

import java.util.UUID;

import no.nordicsemi.puckcentral.bluetooth.gatt.GattCharacteristicReadCallback;

public class GattCharacteristicReadOperation extends GattOperation {
    private final UUID mService;
    private final UUID mCharacteristic;
    private final GattCharacteristicReadCallback mCallback;

    public GattCharacteristicReadOperation(BluetoothDevice device, UUID service, UUID characteristic, GattCharacteristicReadCallback callback) {
        super(device);
        mService = service;
        mCharacteristic = characteristic;
        mCallback = callback;
    }

    @Override
    public void execute(BluetoothGatt gatt) {
        L.d("writing to " + mCharacteristic);
        BluetoothGattCharacteristic characteristic = gatt.getService(mService).getCharacteristic(mCharacteristic);
        if (characteristic == null) {
            L.e("Could not find characteristic: " + mCharacteristic.toString());
        } else {
            gatt.readCharacteristic(characteristic);
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    public void onRead(BluetoothGattCharacteristic characteristic) {

        final byte[] value = characteristic.getValue();
        if (value.length > 0) {
            int intValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            L.e("1st-getIntValue(UINT8,0)=" + intValue);
            String s = characteristic.getStringValue(0);
            L.e("1st-getStringValue(0)=" + s);
        } else {
            L.e("1st-Value is zero length");
        }

        final byte[] value2 = characteristic.getValue();
        if (value.length > 0) {
            int intValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            L.e("2nd-getIntValue(UINT8,0)=" + intValue);
            String s = characteristic.getStringValue(0);
            L.e("2nd-getStringValue(0)=" + s);
        } else {
            L.e("2nd-value is zero length");
        }

        mCallback.call(value);
    }
}
