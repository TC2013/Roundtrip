package com.gxwtech.RileyLink;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by geoff on 7/31/15.
 */
public class RileyLinkUtil {
    private static final String TAG = "RileyLinkUtil";
    /*
     CodeSymbols is an ordered list of translations
     6bits -> 4 bits, in order from 0x0 to 0xF
     The 6 bit codes are what is used on the RF side of the RileyLink
     to communicate with a Medtronic pump.

     The RileyLink translates the 6 bit codes into bytes when receiving,
     but we have to construct the 6 bit codes when sending.
     */
    public static byte[] CodeSymbols = {
            0x15,
            0x31,
            0x32,
            0x23,
            0x34,
            0x25,
            0x26,
            0x16,
            0x1a,
            0x19,
            0x2a,
            0x0b,
            0x2c,
            0x0d,
            0x0e,
            0x1c
    };

    public static byte[] appendChecksum(final byte[] input) {
        if (input == null) {
            return null;
        }
        if (input.length == 0) {
            return null;
        }
        byte[] rval = new byte[input.length+1];
        System.arraycopy(input,0,rval,0,input.length);
        rval[input.length] = CRC.crc8(input);
        return rval;
    }

    public static byte[] xcomposeRFStream(byte[] input) {
        /*
         0xa7 -> (0xa -> 0x2a == 101010) + (0x7 -> 0x16 == 010110) == 1010 1001 0110 = 0xa96
         0x12 -> (0x1 -> 0x31 == 110001) + (0x2 -> 0x32 == 110010) == 1100 0111 0010 = 0xc72

         so:
         {0xa7} -> {0xa9, 0x60}
         {0xa7, 0x12} -> {0xa9, 0x6c, 0x72}
         {0xa7, 0x12, 0xa7} -> {0xa9, 0x6c, 0x72, 0xa9, 0x60}

         */
        byte[] rval = null;
        if (input == null) return rval;
        if (input.length == 0) return rval;
        int outSize = (int)(Math.ceil((input.length * 3.0) / 2.0));
        rval = new byte[outSize+1];
        for (int i=0; i< input.length; i++) {
            int rfBytes = composeRFBytes(input[i]);
            int outIndex = ((i/2) * 3) + (i%2);
            // outIndex: 0->0, 1->1, 2->3, 3->4, 4->6, 5->7, 6->9, 7->10
            if ((i % 2)==0) {
                rval[outIndex] = (byte)(rfBytes >> 8);
                rval[outIndex+1] = (byte)(rfBytes & 0xF0);
            } else {
                rval[outIndex] = (byte)((rval[outIndex] & 0xF0) | ((rfBytes >> 12) & 0x0F));
                rval[outIndex+1] = (byte)(rfBytes >> 4);
            }
        }
        rval[outSize] = 0;
        //rval[outSize+1] = 0;

        /* check that the other algorithm matches */
        Log.e(TAG,"ComposeRFStream: input is " + RileyLinkUtil.toHexString(input));
        Log.e(TAG,"ComposeRFStream: output is " + RileyLinkUtil.toHexString(rval));
        byte[] checkEm = encodeData(input);

        /* these asserts failed.  Why? */
        assert checkEm.length == rval.length;
        for (int i=0; i< checkEm.length; i++) {
            assert checkEm[i] == rval[i];
        }

        return rval;
    }


    // return a 12 bit binary number representing outgoing RF code for byte
    // 0xa7 -> (0xa -> 0x2a == 101010) + (0x7 -> 0x16 == 010110) == 1010 1001 0110 = 0xa96
    // zero extend low bits to achieve 2 bytes: 0xa960
    public static int composeRFBytes(byte b) {
        int rval = 0;

        // bit translations are per nibble
        int lowNibble = b & 0x0F;
        byte lowCode = CodeSymbols[lowNibble];
        int highNibble = (b & 0xF0) >> 4;
        byte highCode = CodeSymbols[highNibble];

        byte highByte = (byte)(((highCode << 2) & 0xFC) | ((lowCode & 0x30) >> 4));
        byte lowByte = (byte)((lowCode & 0x0f) << 4);
        rval = highByte;
        rval = rval << 8;
        rval = rval | (lowByte & 0xFF);
        return rval;
    }

    public static ArrayList<Byte> fromBytes(byte[] data) {
        ArrayList<Byte> rval = new ArrayList<>();
        for (int i=0; i<data.length; i++) {
            rval.add(data[i]);
        }
        return rval;
    }

    public static byte[] toBytes(ArrayList<Byte> data) {
        byte[] rval = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            rval[i] = data.get(i);
        }
        return rval;
    }

/*
    + (NSData*)encodeData:(NSData*)data {
        NSMutableData *outData = [NSMutableData data];
        NSMutableData *dataPlusCrc = [data mutableCopy];
        unsigned char crc = [MinimedPacket crcForData:data];
        [dataPlusCrc appendBytes:&crc length:1];
        char codes[16] = {21,49,50,35,52,37,38,22,26,25,42,11,44,13,14,28};
        const unsigned char *inBytes = [dataPlusCrc bytes];
        unsigned int acc = 0x0;
        int bitcount = 0;
        for (int i=0; i < dataPlusCrc.length; i++) {
            acc <<= 6;
            acc |= codes[inBytes[i] >> 4];
            bitcount += 6;

            acc <<= 6;
            acc |= codes[inBytes[i] & 0x0f];
            bitcount += 6;

            while (bitcount >= 8) {
                unsigned char outByte = acc >> (bitcount-8) & 0xff;
                [outData appendBytes:&outByte length:1];
                bitcount -= 8;
                acc &= (0xffff >> (16-bitcount));
            }
        }
        if (bitcount > 0) {
            acc <<= (8-bitcount);
            unsigned char outByte = acc & 0xff;
            [outData appendBytes:&outByte length:1];
        }
        return outData;
    }
*/

    public static byte[] encodeData(byte[] data) {
        // use arraylists because byte[] is annoying.
        ArrayList<Byte> inData = fromBytes(data);
        ArrayList<Byte> outData = new ArrayList<>();
        /*
        ArrayList<Byte> dataPlusCrc = fromBytes(OtherCRC.appendCRC(data));
        ArrayList<Byte> dataPlusMyCrc = fromBytes(data);
        */
        //dataPlusMyCrc.add(CRC.crc8(data));

        final byte[] codes = new byte[] {21,49,50,35,52,37,38,22,26,25,42,11,44,13,14,28 };
        int acc = 0;
        int bitcount = 0;
        int i;
        for (i=0; i<inData.size(); i++) {
            acc <<= 6;
            acc |= codes[(inData.get(i) >> 4) & 0x0f];
            bitcount += 6;

            acc <<= 6;
            acc |= codes[inData.get(i) & 0x0f];
            bitcount += 6;

            while (bitcount >= 8) {
                byte outByte = (byte)(acc >> (bitcount-8) & 0xff);
                outData.add(outByte);
                bitcount -= 8;
                acc &= (0xffff >> (16 - bitcount));
            }
        }
        if (bitcount > 0) {
            acc <<= (8-bitcount);
            byte outByte = (byte)(acc & 0xff);
            outData.add(outByte);
        }


        // convert back to byte[]
        byte[] rval = toBytes(outData);

        Log.e(TAG,"encodeData: (length " + data.length + ") input is " + toHexString(data));
        //Log.e(TAG,"encodeData: input with OtherCRC is " + toHexString(toBytes(dataPlusCrc)));
        //Log.e(TAG,"encodeData: input with My CRC is " +toHexString(toBytes(dataPlusMyCrc)));
        Log.e(TAG,"encodeData: (length " + rval.length + ") output is " + toHexString(rval));
        return rval;

    }

    //public static byte[] composeRFBitstream(byte[] data) {
    //}
/*
    public static void testCompose(byte[] instream) {
        if (instream == null) return;
        if (instream.length == 0) return;
        int outval = composeRFBytes((byte)0xa7);
        Log.i(TAG, "testCompose: input is " + toHexString(instream) +
                ", output is " + toHexString(outstream));
    }
*/
    public static void test() {
        /*
        {0xa7} -> {0xa9, 0x60}
        {0xa7, 0x12} -> {0xa9, 0x6c, 0x72}
        {0xa7, 0x12, 0xa7} -> {0xa9, 0x6c, 0x72, 0xa9, 0x60}
        */
        //testCompose(new byte[] {(byte)0xa7, (byte)0xa7});
        int result;
        result = composeRFBytes((byte)0xa7);

        byte[] bs = encodeData(new byte[]{(byte) 0xa7});
        bs = encodeData(new byte[]{(byte) 0xa7, 0x12});
        bs = encodeData(new byte[]{(byte) 0xa7, 0x12, (byte) 0xa7});
        return;
    }

    public static String toHexString(byte[] array) {
        return toHexString(array, 0, array.length);
    }

    private final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];

        int bufIndex = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = array[i];
            buf[bufIndex++] = HEX_DIGITS[(b >>> 4) & 0x0F];
            buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
        }

        return new String(buf);
    }


}
