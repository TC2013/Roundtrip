package com.gxwtech.rtdemo.carelink;

/**
 * Created by geoff on 4/27/15.
 */
public enum CarelinkCommandEnum {
    // Add others as needed
    CMD_C_INVALID((byte) 0),  // GGW: I just made this one up...
    CMD_C_TRANSMIT_PACKET((byte) 1),
    CMD_C_READ_CONTINUE((byte) 2), // GGW: I just made this one up...
    CMD_C_LINK_STATUS((byte) 3),
    CMD_C_PRODUCT_INFO((byte) 4),
    CMD_C_LINK_STATS((byte) 5),
    CMD_C_SIGNAL_STRENGTH((byte) 6),
    CMD_C_READ_RADIO((byte) 0x0c);

    // The following are all wrong.  Need new ones.
    /*
    CMD_INITIALIZE           ((byte)1),
    CMD_READ_STATUS          ((byte)2),
    CMD_SELF_TEST            ((byte)3),
    CMD_C_TRANSMIT_PACKET      ((byte)4),
    CMD_TRANSMIT_LAST_PACKET ((byte)5),
    CMD_RS232_MODE_SELECT    ((byte)6),
    CMD_RF_MODE_SELECT       ((byte)7),
    CMD_TRANSFER_DATA        ((byte)8),
    CMD_SET_FEATURES         ((byte)9),
    CMD_MULTI_TRANSMIT       ((byte)10);
    */
    private byte opcode;

    public byte opcode() {
        return opcode;
    }

    CarelinkCommandEnum(byte b) {
        opcode = b;
    }
}
