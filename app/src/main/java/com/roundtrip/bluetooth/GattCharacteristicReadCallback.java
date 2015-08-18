package com.roundtrip.bluetooth;

public interface GattCharacteristicReadCallback {
    void call(byte[] characteristic);
}
