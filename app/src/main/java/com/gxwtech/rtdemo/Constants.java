package com.gxwtech.rtdemo;

/**
 * Created by geoff on 4/10/15.
 */
public interface Constants {
    class ACTION {
        public static String GENERIC_ACTION = "com.gxwtech.rtdemo.action.GENERIC_ACTION";
        public static String START_RT_ACTION = "com.gxwtech.rtdemo.action.START_RT_ACTION";
        public static int UPDATE_LOG_LISTVIEW = 200;
    }
    class NOTIFICATION_ID {
        public static int RT_NOTIFICATION = 111;
    }
    // SRQ is service requests
    // these are codes passed from foreground (MainActivity)
    // to background (RTDemoService)
    class SRQ {
        public static String START_SERVICE = "StartService"; // 301
        public static String SRQ_UNUSED = "UNUSED-302"; // 302;
        public static String VERIFY_PUMP_COMMUNICATIONS = "VerifyPumpCommunications"; //303;

        public static String VERIFY_DB_ACCESS = "VerifyDBAccess"; //304;
        // report_pump_settings sends back a PumpSettingsParcel
        public static String REPORT_PUMP_SETTINGS = "ReportPumpSettings"; //305;
        // SET_SERIAL_NUMBER takes arg2 as a byte[3] array;
        //public static String SET_SERIAL_NUMBER = "SetSerialNumber"; //306;
        // REPORT_PUMP_HISTORY should take a "minutes" argument, but doesn't yet.
        public static String REPORT_PUMP_HISTORY = "ReportPumpHistory"; //307;
        // SET_TEMP_BASAL needs a double insulinUnits and an int durationMinutes
        // Pass as a parcel, using TempBasalPairParcel
        public static String SET_TEMP_BASAL = "SetTempBasal"; //308;
        // APSLOGIC_STARTUP requests the APSLogic module to do the
        // initial data collection, which can take a long time (MongoDB access, pump access)
        // and to run the MakeADecision loop once.
        public static String APSLOGIC_STARTUP = "APSLogicStartup"; //309;
        // MongoDBSettingsActivity fires this off to announce new settings for the DB URI
        public static String MONGO_SETTINGS_CHANGED = "MongoSettingsChanged"; //310;
        // PersonalProfileActivity fires this off when the ISF number has been set.
        // public static int SET_ISF = 311;
        // PersonalProfileActivity fires this off when the CAR number has been set.
        public static String PERSONAL_PREFERENCE_CHANGE = "PersonalPreferencesChanged"; //312;
        // SuspendAPSActivity sends this when user has requested a suspend
        public static String DO_SUSPEND_MINUTES = "DoSuspendMinutes"; //313;
        // MonitorActivity start button runs this.
        public static String START_REPEAT_ALARM = "StartRepeatingAlarm"; //314;
        // MonitorActivity stop button runs this.
        public static String STOP_REPEAT_ALARM = "StopRepeatingAlarm"; //315;
    }

    class ParcelName {
        public static String PumpSettingsParcelName = "PumpSettingsParcel";
        public static String TempBasalPairParcelName = "TempBasalPairParcel";
        public static String BGReadingParcelName = "BGReadingParcel";
    }
    class PreferenceID {
        // Name of a SharedPreference collection
        public static String MainActivityPrefName = "MainActivityPreferences";
    }
    class PrefName {
        // Name of an entry in a SharedPreference collection
        public static String SerialNumberPrefName = "PumpSerialNumber";
        public static String MongoDBServerPrefName = "MongoDBServerAddress";
        public static String MongoDBServerPortPrefName = "MongoDBServerPort";
        public static String MongoDBDatabasePrefName = "MongoDBDatabase";
        public static String MongoDBUsernamePrefName = "MongoDBUsername";
        public static String MongoDBPasswordPrefName = "MongoDBPassword";
        public static String MongoDBCollectionPrefName = "MongoDBCollection";
        public static String MongoDBAllowWritingToDBPrefName = "MongoDBAllowWrites";
        public static String SuspendMinutesPrefName = "SuspendAPSMinutes";
        // and all kinds of other goodies:
        public static String LastPowerControlRunTime = "LastPowerControlRunTime";

        // These have been moved to PreferenceBackedStorage.  TODO: move all others there
        public static String LatestBGTimestamp = "LatestBGTimestamp";
        public static String LatestBGReading = "LatestBGReading";
        public static String LowGlucoseSuspendPoint = "LowGlucoseSuspendPoint";
        public static String CARPrefName = "CarbAbsorptionRatio";
        public static String CarbDelayPrefName = "CarbDelayMinutes";
        public static String PPTargetBGPrefName = "TargetBG";
        public static String PPBGMaxPrefName = "TargetBGMax";
        public static String PPBGMinPrefName = "TargetBGMin";
        public static String PPMaxTempBasalRatePrefName = "MaxTempBasalRate";
        public static String PPNormalDIATable = "NormalDIATable";
        public static String PPNegativeInsulinDIATable = "NegativeInsulinDIATable";

        // These are in PreferenceBackedStorage, used for communication from apslogic to monitorActivity
        public static String Monitor_TempBasalRate = "Monitor_TempBasalRate";
        public static String Monitor_TempBasalDuration = "Monitor_TempBasalDuration";
        public static String Monitor_CurrBasalRate = "Monitor_CurrBasalRate";
        public static String Monitor_PredBG = "Monitor_PredBG";
        public static String Monitor_IOB = "Monitor_IOB";
        public static String Monitor_COB = "Monitor_COB";

    }
}
