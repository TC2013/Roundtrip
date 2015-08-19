package com.roundtrip;

/**
 * A class just to contain all our Intent strings
 */
public class Intents {
    private static final String basename = "com.roundtrip";

    // these are intents sent from background service to foreground service
    public static final String ROUNDTRIP_STATUS_MESSAGE = basename + ".rtstatusmsg";
    public static final String ROUNDTRIP_STATUS_MESSAGE_STRING = basename + ".rtstatusmsg_string";
    public static final String ROUNDTRIP_SLEEP_MESSAGE = basename + ".rtsleepmsg";
    public static final String ROUNDTRIP_SLEEP_MESSAGE_DURATION = basename + ".rtsleepmsg_duration";
    public static final String ROUNDTRIP_TASK_RESPONSE = basename + ".rttaskresponse";
    public static final String ROUNDTRIP_BG_READING = basename + ".bgreading";
    public static final String APSLOGIC_LOG_MESSAGE = basename + ".apslogic_log_msg";
    public static final String MONITOR_DATA_CHANGED = basename + ".monitor_data_changed";

    public static final String RILEYLINK_CONNECTED = basename + ".rileylink_connected";
    public static final String RILEYLINK_CONNECTING = basename + ".rileylink_connecting";
    public static final String RILEYLINK_DISCONNECTED = basename + ".rileylink_disconnected";
    public static final String RILEYLINK_BATTERY_UPDATE = basename + ".rileylink_battery";

    public static final String ENLITE_SENSOR_UPDATE = basename + ".enlite_sensor_update";
}
