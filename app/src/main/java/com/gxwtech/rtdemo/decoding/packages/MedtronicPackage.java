package com.gxwtech.rtdemo.decoding.packages;

/**
 * Created by fokko on 6-8-15.
 */
public abstract class MedtronicPackage {
    public abstract void decode(byte[] data);

    protected abstract int packageLength();
}