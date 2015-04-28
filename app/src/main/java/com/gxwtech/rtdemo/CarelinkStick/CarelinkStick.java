package com.gxwtech.rtdemo.CarelinkStick;

/**
 * Created by geoff on 4/24/15.
 */
public class CarelinkStick {
    DeviceTransport mTransport;
    boolean m_download_i = false;
}

/*
 * A minimed packet (read RTC) looks like this:
 *
 * 0000   0x01 0x00 0xa7 0x01 0x46 0x73 0x24 0x80    ....Fs$.
 * 0008   0x00 0x00 0x02 0x01 0x00 0x70 0x7c 0x00    .....p|.
 *
 * the first four bytes are a request to the carelink stick to transmit a packet:
 * 0x01 0x00 0xa7 0x01
 *
 * Next three bytes are the pump's serial number: 0x46 0x73 0x24
 *
 * That leaves:
 * 0x80 0x00 0x00 0x02 0x01 0x00 0x70 0x7c 0x00
 * 0x80 0x00 is a 15 bit (almost 2 byte) representation of the number of parameters that are being sent. high byte is first, ORed with 0x80, then low byte.
 *
 * that leaves:
 * 0x00 0x02 0x01 0x00 0x70 0x7c 0x00
 *
 * Don't know what the next byte is, except that for "COMMAND 93" it needs to be a (decimal) 85, and zero otherwise.
 * 0x00
 *
 * Next (byte?) is maxRetries (default to 5? 2?)
 * 0x02
 * In Ben's code, this is specified with the command (a member of the command)
 *
 * Then we calculate the number of packets/framges/pages/flows that this command will take (really only 1 or 2?)
 * 0x01
 * (is this also specified in Ben's code, as maxRecords in the command?)
 * this is the number of 64 byte packets that are required to contain a response from the pump. depends on what we request (but really either 1 or 2)
 *
 * That leaves:
 *  0x00 0x70 0x7c 0x00
 *
 * Then write a zero (?)
 * 0x00
 *
 * that leaves:
 *  0x70 0x7c 0x00
 *
 * Next write the command code (read RTC) (0x70?)
 * 0x70
 * These are listed in decocare/commands.py, if nowhere else.
 *
 * that leaves 0x7c 0x00
 * Then append a CRC8 for the packet (which packet?)
 * 0x7c
 *
 * Then write the params (none, in this case) (0 bytes)
 *
 * Then write the CRC8 for the params (0x00 in this example, as there are none)
 * 0x00
 *
 * ---------------------------------------------------
 * do it again for another packet (read history data)
 * packet is:
 * 0000   0x01 0x00 0xa7 0x01 0x46 0x73 0x24 0x80    ....Fs$.
 * 0008   0x01 0x00 0x02 0x02 0x00 0x80 0xf6 0x00    ........
 * 0010   0x00                                       .
 *
 * Carelink header, to request the minimed packet to be sent
 * 0x01 0x00 0xa7 0x01
 *
 * that leaves:
 *  0x46 0x73 0x24 0x80 0x01 0x00 0x02 0x02 0x00 0x80 0xf6 0x00 0x00
 *
 * 0x46 0x73 0x24 is pump serial number
 * that leaves:
 *  0x80 0x01 0x00 0x02 0x02 0x00 0x80 0xf6 0x00 0x00
 * number of params (0x80 0x01) means 1 param
 *  0x00 0x02 0x02 0x00 0x80 0xf6 0x00 0x00
 *
 * next is unknown, but not command 93, so zero. next is maxRetries (2, in commands.py)
 * that leaves:
 * 0x02 0x00 0x80 0xf6 0x00 0x00
 *
 * Next is the number of records required.  This is bytesPerRecord * maxRecords / 64 (round up), so either 1 or two. 2 in this case
 * that leaves:
 * 0x80 0xf6 0x00 0x00
 *
 * Next is the ReadHistoryData command code (0x80)
 * that leaves:
 * 0xf6 0x00 0x00
 *
 * Next is CRC8, 0xf6
 * next is the first param (0x00), meaning page zero (1 byte)
 *
 * Then CRC8 of the params, which, since the only param is a zero, the CRC8 is zero. (1 byte)
 *
 * ---------------------
 * packet:
 * Carelink header (4 bytes)
 *  pump serial (3 bytes)
 *  number of params (2 bytes)
 *  0 or 93 (1 byte)
 *  number of records (1 or 2) (1 byte)
 *  command code (1 byte)
 *  CRC8 of packet (1 byte)
 *  parameter bytes (n bytes)
 *  CRC8 of params (1 byte)
 *
 *  Now, reading the responses!
 *
 *  Got this back from readRTC:
 *
 *  0x00000000 01 55 00 09 0B 00 00 00 00 43 6F 6D 4C 69 6E 6B .U.......ComLink
 *  0x00000010 20 49 49 01 10 02 00 01 01 03 00 00 00 00 00 00 .II.............
 *  0x00000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
 *  0x00000030 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 *
 * The first byte is "commStatus", which we expect to be 0x01
 * the second byte is "status" which we expect to be 0x55 (85, 'U')
 *
 * this is apparently some sort of "product info" response, not what we asked for.
 * gotta touch carelink right, first, to get it to send our stuff?
 *
 * From Ben's decocare:
 * 0000   0x01 0x55 0x00 0x09 0x0b 0x00 0x00 0x00    .U......
 * 0008   0x00 0x43 0x6f 0x6d 0x4c 0x69 0x6e 0x6b    .ComLink
 * 0010   0x20 0x49 0x49 0x01 0x10 0x02 0x00 0x01     II.....
 * 0018   0x01 0x03 0x00 0x00 0x00 0x00 0x00 0x00    ........
 * 0020   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
 * 0028   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
 * 0030   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
 * 0038   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
 *
 * This exactly matches the packet response for a "read product info" command. how strange.
 * Maybe it's a leftover packet from the last carelink-talking-app I ran?
 *
 * Seems we have to get the signal strength first? "init"? See decocare/stick.py line 150 (or so)
 *
 **************/
