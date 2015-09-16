package com.gxwtech.RileyLink;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

import no.nordicsemi.puckcentral.bluetooth.gatt.GattCharacteristicReadCallback;
import no.nordicsemi.puckcentral.bluetooth.gatt.GattManager;
import no.nordicsemi.puckcentral.bluetooth.gatt.operations.GattCharacteristicReadOperation;

/**
 * Created by geoff on 8/13/15.
 */
public class ReadBatteryLevelCommand extends RileyLinkCommand {
    public ReadBatteryLevelCommand(BluetoothDevice device, GattManager gattManager, GattCharacteristicReadCallback cb) {
        super(device,gattManager);
        mDevice = device;
        mGattManager = gattManager;
        bundle.addOperation(new GattCharacteristicReadOperation(getDevice(), UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_UUID),cb));
    }
}
