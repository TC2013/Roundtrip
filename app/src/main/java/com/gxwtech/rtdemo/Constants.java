package com.gxwtech.rtdemo;

/**
 * Created by Geoff on 4/10/15.
 */
public interface Constants {
    class ACTION {
        public static final String GENERIC_ACTION = "com.gxwtech.rtdemo.action.GENERIC_ACTION";
        public static final String START_RT_ACTION = "com.gxwtech.rtdemo.action.START_RT_ACTION";
        public static final int UPDATE_LOG_LISTVIEW = 200;
    }

    class NOTIFICATION_ID {
        public static final int RT_NOTIFICATION = 111;
    }

    // SRQ is service requests
    // these are codes passed from foreground (MainActivity)
    // to background (RTDemoService)
    class SRQ {
        public static final String START_SERVICE = "StartService"; // 301
        public static final String SRQ_UNUSED = "UNUSED-302"; // 302;


        public static final String BLUETOOTH_CONNECT = "bluetoothConnect"; //303;
        public static final String BLUETOOTH_READ = "bluetoothWrite"; //303;
        public static final String BLUETOOTH_WRITE  = "bluetoothRead"; //303;

        public static final String VERIFY_DB_ACCESS = "VerifyDBAccess"; //304;
        // report_pump_settings sends back a PumpSettingsParcel
        public static final String REPORT_PUMP_SETTINGS = "ReportPumpSettings"; //305;
        // SET_SERIAL_NUMBER takes arg2 as a byte[3] array;
        //public static String SET_SERIAL_NUMBER = "SetSerialNumber"; //306;
        // REPORT_PUMP_HISTORY should take a "minutes" argument, but doesn't yet.
        public static final String REPORT_PUMP_HISTORY = "ReportPumpHistory"; //307;
        // SET_TEMP_BASAL needs a double insulinUnits and an int durationMinutes
        // Pass as a parcel, using TempBasalPairParcel
        public static final String SET_TEMP_BASAL = "SetTempBasal"; //308;
        // APSLOGIC_STARTUP requests the APSLogic module to do the
        // initial data collection, which can take a long time (MongoDB access, pump access)
        // and to run the MakeADecision loop once.
        public static final String APSLOGIC_STARTUP = "APSLogicStartup"; //309;
        // MongoDBSettingsActivity fires this off to announce new settings for the DB URI
        public static final String MONGO_SETTINGS_CHANGED = "MongoSettingsChanged"; //310;
        // PersonalProfileActivity fires this off when the ISF number has been set.
        // public static int SET_ISF = 311;
        // PersonalProfileActivity fires this off when the CAR number has been set.
        public static final String PERSONAL_PREFERENCE_CHANGE = "PersonalPreferencesChanged"; //312;
        // SuspendAPSActivity sends this when user has requested a suspend
        public static final String DO_SUSPEND_MINUTES = "DoSuspendMinutes"; //313;
        // MonitorActivity start button runs this.
        public static final String START_REPEAT_ALARM = "StartRepeatingAlarm"; //314;
        // MonitorActivity stop button runs this.
        public static final String STOP_REPEAT_ALARM = "StopRepeatingAlarm"; //315;
    }

    class ParcelName {
        public static final String PumpSettingsParcelName = "PumpSettingsParcel";
        public static final String TempBasalPairParcelName = "TempBasalPairParcel";
        public static final String BGReadingParcelName = "BGReadingParcel";
    }

    class PreferenceID {
        // Name of a SharedPreference collection
        public static final String MainActivityPrefName = "MainActivityPreferences";
    }

    class PrefName {
        // Name of an entry in a SharedPreference collection
        public static final String SerialNumberPrefName = "PumpSerialNumber";
        public static final String MongoDBServerPrefName = "MongoDBServerAddress";
        public static final String MongoDBServerPortPrefName = "MongoDBServerPort";
        public static final String MongoDBDatabasePrefName = "MongoDBDatabase";
        public static final String MongoDBUsernamePrefName = "MongoDBUsername";
        public static final String MongoDBPasswordPrefName = "MongoDBPassword";
        public static final String MongoDBCollectionPrefName = "MongoDBCollection";
        public static final String MongoDBAllowWritingToDBPrefName = "MongoDBAllowWrites";
        public static final String SuspendMinutesPrefName = "SuspendAPSMinutes";
        // and all kinds of other goodies:
        public static final String LastPowerControlRunTime = "LastPowerControlRunTime";

        // These have been moved to PreferenceBackedStorage.  TODO: move all others there
        public static final String LatestBGTimestamp = "LatestBGTimestamp";
        public static final String LatestBGReading = "LatestBGReading";
        public static final String LowGlucoseSuspendPoint = "LowGlucoseSuspendPoint";
        public static final String CARPrefName = "CarbAbsorptionRatio";
        //public static String CarbDelayPrefName = "CarbDelayMinutes";
        public static final String ISFPrefName = "InsulinSensitivityFactor";
        public static final String PPTargetBGPrefName = "TargetBG";
        public static final String PPBGMaxPrefName = "TargetBGMax";
        public static final String PPBGMinPrefName = "TargetBGMin";
        public static final String PPMaxTempBasalRatePrefName = "MaxTempBasalRate";
        public static final String PPNormalDIATable = "NormalDIATable";
        public static final String PPNegativeInsulinDIATable = "NegativeInsulinDIATable";
        public static final String LoggingEnabled = "LoggingEnabled";
        public static final String KeepLogsForHours = "KeepLogsForHours";

        // These are in PreferenceBackedStorage, used for communication from apslogic to monitorActivity
        public static final String Monitor_TempBasalRate = "Monitor_TempBasalRate";
        public static final String Monitor_TempBasalDuration = "Monitor_TempBasalDuration";
        public static final String Monitor_CurrBasalRate = "Monitor_CurrBasalRate";
        public static final String Monitor_PredBG = "Monitor_PredBG";
        public static final String Monitor_IOB = "Monitor_IOB";
        public static final String Monitor_COB = "Monitor_COB";

        public static final String Bluetooth_RileyLink_Address = "00:07:80:39:4C:B5";
    }
}
