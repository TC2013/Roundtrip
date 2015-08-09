package com.gxwtech.rtdemo.decoding;

/**
 * Created by fokko on 6-8-15.
 */
public abstract class DataPackage {
    public abstract void decode(byte[] data);
    protected abstract int packageLength();
}