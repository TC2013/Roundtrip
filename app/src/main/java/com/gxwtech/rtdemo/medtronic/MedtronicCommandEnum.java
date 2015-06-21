package com.gxwtech.rtdemo.medtronic;

/**
 * Created by geoff on 4/28/15.
 */
public enum MedtronicCommandEnum {
    CMD_M_INVALID_CMD ((byte)255),  // ggw: I just made this one up...
    CMD_M_READ_PUMP_STATUS((byte)206), // ?
    CMD_M_BOLUS ((byte)66),
    //
    CMD_M_READ_INSULIN_REMAINING((byte)115),
    CMD_M_READ_HISTORY ((byte)128),
    CMD_M_POWER_CTRL ((byte)93),
    CMD_M_BEGIN_PARAMETER_SETTING ((byte)38),
    CMD_M_END_PARAMETER_SETTING ((byte)39),
    CMD_M_TEMP_BASAL_RATE ((byte)76),
    CMD_M_READ_ERROR_STATUS ((byte)117),
    CMD_M_READ_FIRMWARE_VER ((byte)116),
    CMD_M_READ_PUMP_ID ((byte)113),
    CMD_M_READ_PUMP_STATE ((byte)131),
    CMD_M_READ_REMOTE_CTRL_IDS ((byte)118),
    CMD_M_READ_TEMP_BASAL ((byte)152),
    CMD_M_READ_RTC ((byte)112),
    CMD_M_SET_RF_REMOTE_ID ((byte)81),
    CMD_M_SET_ALERT_TYPE ((byte)84),
    CMD_M_SET_AUTO_OFF ((byte)78),
    CMD_M_SET_BLOCK_ENABLE ((byte)82),
    CMD_M_SET_CURRENT_PATTERN ((byte)74),
    CMD_M_SET_EASY_BOLUS_ENABLE ((byte)79),
    CMD_M_SET_MAX_BOLUS ((byte)65),
    CMD_M_SET_PATTERNS_ENABLE ((byte)85),
    CMD_M_SET_RF_ENABLE ((byte)87),
    CMD_M_SET_RTC ((byte)64),
    CMD_M_KEYPAD_PUSH ((byte)91),
    CMD_M_SET_TIME_FORMAT ((byte)92),
    CMD_M_SET_VAR_BOLUS_ENABLE ((byte)69),
    CMD_M_READ_STD_PROFILES ((byte)146),
    CMD_M_READ_A_PROFILES ((byte)147),
    CMD_M_READ_B_PROFILES ((byte)148),
    CMD_M_READ_SETTINGS ((byte)145),
    CMD_M_SET_STD_PROFILE ((byte)111),
    CMD_M_SET_A_PROFILE ((byte)48),
    CMD_M_SET_B_PROFILE ((byte)49),
    CMD_M_SET_MAX_BASAL ((byte)110),
    CMD_M_READ_BG_ALARM_CLOCKS ((byte)142),
    CMD_M_READ_BG_ALARM_ENABLE ((byte)151),
    CMD_M_READ_BG_REMINDER_ENABLE ((byte)144),
    CMD_M_READ_BG_TARGETS ((byte)140),
    CMD_M_READ_BG_UNITS ((byte)137),
    CMD_M_READ_BOLUS_WIZARD_SETUP_STATUS ((byte)135),
    CMD_M_READ_CARB_RATIOS ((byte)138),
    CMD_M_READ_CARB_UNITS ((byte)136),
    CMD_M_READ_LOGIC_LINK_IDS ((byte)149),
    CMD_M_READ_INSULIN_SENSITIVITIES ((byte)139),
    CMD_M_READ_RESERVOIR_WARNING ((byte)143),
    CMD_M_READ_PUMP_MODEL_NUMBER ((byte)141),
    CMD_M_SET_BG_ALARM_CLOCKS ((byte)107),
    CMD_M_SET_BG_ALARM_ENABLE ((byte)103),
    CMD_M_SET_BG_REMINDER_ENABLE ((byte)108),
    CMD_M_SET_BOLUS_WIZARD_SETUP ((byte)94),
    CMD_M_SET_INSULIN_ACTION_TYPE ((byte)88),
    CMD_M_SET_LOGIC_LINK_ENABLE ((byte)51),
    CMD_M_SET_LOGIC_LINK_ID ((byte)50),
    CMD_M_SET_RESERVOIR_WARNING ((byte)106),
    CMD_M_SET_TEMP_BASAL_TYPE ((byte)104),
    CMD_M_SUSPEND_RESUME ((byte)77),
    CMD_M_PACKET_LENGTH ((byte)7),
    CMD_M_READ_PUMP_SETTINGS ((byte)192);

    byte opcode;

    MedtronicCommandEnum(byte b) {
        opcode = b;
    }

}
