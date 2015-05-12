package com.gxwtech.rtdemo.Carelink.util;

/**
 * Created by geoff on 4/28/15.
 */
public class ByteUtil {
    private final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    public static byte highByte(short s) {
        return (byte)(s/256);
    }
    public static byte lowByte(short s) {
        return (byte)(s%256);
    }

    public static byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static byte[] concat(byte[] a, byte b) {
        int aLen = a.length;
        byte[] c = new byte[aLen + 1];
        System.arraycopy(a,0,c,0,aLen);
        c[aLen] = b;
        return c;
    }
    public static byte[] substring(byte[] a,int start, int len) {
        byte[] rval = new byte[len];
        System.arraycopy(a,start,rval,0,len);
        return rval;
    }
    public static String shortHexString(byte[] ra) {
        String rval = "";
        if (ra == null) { return rval; }
        if (ra.length == 0) { return rval; }
        for (int i=0; i< ra.length; i++) {
            rval = rval + HEX_DIGITS[(ra[i] & 0xF0) >> 4];
            rval = rval + HEX_DIGITS[(ra[i] & 0x0F)];
            if (i< ra.length-1) {
                rval = rval + " ";
            }
        }
        return rval;
    }

}
