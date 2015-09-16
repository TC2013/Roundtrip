package com.gxwtech.rtdemo.bluetooth;

/**
 * Created by Fokko on 9-8-15.
 */
public interface GattDescriptorReadCallback {
    void call(byte[] value);
}
