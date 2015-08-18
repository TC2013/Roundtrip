package com.roundtrip.bluetooth;

public class Commands {

    /*
     Based on: https://github.com/bewest/decoding-carelink/blob/master/decocare/commands.py#L1441-L1461
     */
    public static byte[] getReadPumpCommand(final byte[] serial) {
        byte[] data = new byte[]{
                0x01, // head
                0x00, // head
                (byte) 0xA7, // 167
                0x01, // 1
                0x00, // Serial
                0x00, // Serial
                0x00, // Serial
                (byte) 0x80, // 128
                0x00, // 0 Dont know
                0x00, // 0 Dont know
                0x02, // 2 Retries
                0x01, // 1 Pages
                0x00, // 0 Dont know
                (byte) 0x8D, // 141 // Command
                0x00, // CRC
                0x00  // 0
        };

        // Serial
        data[4] = serial[0];
        data[5] = serial[1];
        data[6] = serial[2];

        data[14] = CRC.computeCRC8(data, 14);

        return data;
    }

    public static byte[] setSuspendCommand(final byte[] serial) {
        byte[] data = new byte[]{
                0x00, // Serial
                0x00, // Serial
                0x00, // Serial
        };

        // Serial
        data[0] = serial[0];
        data[1] = serial[1];
        data[2] = serial[2];

        data[14] = CRC.computeCRC8(data, 14);

        return data;
    }
}
