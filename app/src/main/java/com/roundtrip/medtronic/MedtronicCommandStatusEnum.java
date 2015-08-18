package com.roundtrip.medtronic;

// don't know what we're really doing with this one yet.
// for now, copy the CarelinkCommandStatusEnum
public enum MedtronicCommandStatusEnum {
    NONE,
    ACK,
    NACK,
    ERROR_USB // used for UsbExceptions
}
