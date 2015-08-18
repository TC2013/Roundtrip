package com.roundtrip.bluetooth;

public interface GattDescriptorReadCallback {
    void call(byte[] value);
}
