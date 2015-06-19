package com.gxwtech.rtdemo;

/**
 * Created by geoff on 4/16/15.
 * A class just to contain all our Intent strings
 */
public class Intents {
    private static final String basename = "com.gxwtech.roundtrip";

    // these are intents sent from background service to foreground service
    public static final String ROUNDTRIP_STATUS_MESSAGE = basename + ".rtstatusmsg";
    public static final String ROUNDTRIP_STATUS_MESSAGE_STRING = basename + ".rtstatusmsg_string";
    public static final String ROUNDTRIP_SLEEP_MESSAGE = basename + ".rtsleepmsg";
    public static final String ROUNDTRIP_SLEEP_MESSAGE_DURATION = basename + ".rtsleepmsg_duration";
    public static final String ROUNDTRIP_TASK_RESPONSE = basename + ".rttaskresponse";
    public static final String ROUNDTRIP_BG_READING = basename + ".bgreading";
    public static final String APSLOGIC_LOG_MESSAGE = basename + ".apslogic_log_msg";
    public static final String APSLOGIC_IOB_UPDATE = basename + ".apslogic_iob_update";
    public static final String APSLOGIC_COB_UPDATE = basename + ".apslogic_cob_update";
    public static final String APSLOGIC_CURRBASAL_UPDATE = basename + ".apslogic_currbasal_update";
    public static final String APSLOGIC_PREDBG_UPDATE = basename + ".apslogic_predbg_update";
    public static final String APSLOGIC_TEMPBASAL_UPDATE = basename + ".apslogic_tempbasal_update";
    public static final String APSLOGIC_DO_SUSPEND_MINUTES = basename + ".apslogic_do_suspend_minutes";

}
