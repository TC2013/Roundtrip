package com.gxwtech.rtdemo.bluetooth;

/**
 * Created by fokko on 9-8-15.
 */
public interface GattCharacteristicReadCallback {
    void call(byte[] characteristic);
}
