package com.gxwtech.rtdemo;

/**
 * Created by geoff on 4/10/15.
 */
public interface Constants {
    public class ACTION {
        public static String GENERIC_ACTION = "com.gxwtech.rtdemo.action.GENERIC_ACTION";
        public static String START_RT_ACTION = "com.gxwtech.rtdemo.action.START_RT_ACTION";
        public static int UPDATE_LOG_LISTVIEW = 200;
    }
    public class NOTIFICATION_ID {
        public static int RT_NOTIFICATION = 111;
    }
    // SRQ is service requests
    // these are codes passed from foreground (MainActivity)
    // to background (RTDemoService)
    public class SRQ {
        public static int START_SERVICE = 301;
        public static int SRQ_UNUSED = 302;
        public static int VERIFY_PUMP_COMMUNICATIONS = 303;
        public static int VERIFY_DB_ACCESS = 304;
        // report_pump_settings sends back a PumpSettingsParcel
        public static int REPORT_PUMP_SETTINGS = 305;
        // SET_SERIAL_NUMBER takes arg2 as a byte[3] array;
        public static int SET_SERIAL_NUMBER = 306;
        // REPORT_PUMP_HISTORY should take a "minutes" argument, but doesn't yet.
        public static int REPORT_PUMP_HISTORY = 307;
        // SET_TEMP_BASAL needs a double insulinUnits and an int durationMinutes
        // Pass as a parcel, using TempBasalPairParcel
        public static int SET_TEMP_BASAL = 308;
        // APSLOGIC_STARTUP requests the APSLogic module to do the
        // initial data collection, which can take a long time (MongoDB access, pump access)
        public static int APSLOGIC_STARTUP = 309;
        // MongoDBSettingsActivity fires this off to announce new settings for the DB URI
        public static int MONGO_SETTINGS_CHANGED = 310;
        // PersonalProfileActivity fires this off when the ISF number has been set.
        public static int SET_ISF = 311;
        // PersonalProfileActivity fires this off when the CAR number has been set.
        public static int SET_CAR = 312;
    }

    public class ParcelName {
        public static String PumpSettingsParcelName = "PumpSettingsParcel";
        public static String TempBasalPairParcelName = "TempBasalPairParcel";
        public static String BGReadingParcelName = "BGReadingParcel";
    }
    public class PreferenceID {
        // Name of a SharedPreference collection
        public static String MainActivityPrefName = "MainActivityPreferences";
    }
    public class PrefName {
        // Name of an entry in a SharedPreference collection
        public static String SerialNumberPrefName = "PumpSerialNumber";
        public static String ISFPrefName = "InsulinSensitivityFactor";
        public static String CARPrefName = "CarbAbsorptionRatio";
        public static String MongoDBServerPrefName = "MongoDBServerAddress";
        public static String MongoDBServerPortPrefName = "MongoDBServerPort";
        public static String MongoDBDatabasePrefName = "MongoDBDatabase";
        public static String MongoDBUsernamePrefName = "MongoDBUsername";
        public static String MongoDBPasswordPrefName = "MongoDBPassword";
        public static String MongoDBCollectionPrefName = "MongoDBCollection";
    }

}
