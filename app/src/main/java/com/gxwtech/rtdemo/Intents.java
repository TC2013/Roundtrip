package com.gxwtech.rtdemo;

/**
 * Created by geoff on 4/16/15.
 * A class just to contain all our Intent strings
 */
public class Intents {
    private static final String basename = "com.gxwtech.roundtrip";
    // these are intents sent from background service to foreground service
    public static final String ROUNDTRIP_STATUS_MESSAGE = basename + "rtstatusmsg";
    public static final String ROUNDTRIP_STATUS_MESSAGE_STRING = basename + "rtstatusmsg_string";
    public static final String ROUNDTRIP_TASK_RESPONSE = basename + "rttaskresponse";
}
