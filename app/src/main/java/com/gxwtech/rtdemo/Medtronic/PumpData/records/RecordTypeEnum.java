package com.gxwtech.rtdemo.Medtronic.PumpData.records;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by geoff on 5/28/15.
 */
public enum RecordTypeEnum {
    RECORD_TYPE_NULL((byte)0x00, null),
    RECORD_TYPE_BOLUS((byte)0x01,Bolus.class),
    RECORD_TYPE_PRIME((byte)0x03,Prime.class),
    RECORD_TYPE_NODELIVERYALARM((byte)0x06,NoDeliveryAlarm.class),
    RECORD_TYPE_ENDRESULTSTOTALS((byte)0x07,EndResultsTotals.class),
    RECORD_TYPE_CHANGEBASALPROFILE_OLD((byte)0x08,ChangeBasalProfile.class),
    RECORD_TYPE_CHANGEBASALPROFILE_NEW((byte)0x09,ChangeBasalProfile.class), // same? really? see decocare
    RECORD_TYPE_CALBGFORPH((byte)0x0A,CalBgForPh.class),
    RECORD_TYPE_CLEARALARM((byte)0x0C,ClearAlarm.class),
    RECORD_TYPE_SELECTBASALPROFILE((byte)0x14,SelectBasalProfile.class),
    RECORD_TYPE_TEMPBASALDURATION((byte)0x16,TempBasalDuration.class),
    RECORD_TYPE_CHANGETIME((byte)0x17,ChangeTime.class),
    RECORD_TYPE_NEWTIMESET((byte)0x18,NewTimeSet.class),
    RECORD_TYPE_LOWBATTERY((byte)0x19,LowBattery.class),
    RECORD_TYPE_BATTERYACTIVITY((byte)0x1A,BatteryActivity.class),
    RECORD_TYPE_PUMPSUSPENDED((byte)0x1E,PumpSuspended.class),
    RECORD_TYPE_PUMPRESUMED((byte)0x1F,PumpResumed.class),
    RECORD_TYPE_REWOUND((byte)0x21,Rewound.class),
    RECORD_TYPE_TOGGLEREMOTE((byte)0x26,ToggleRemote.class),
    RECORD_TYPE_CHANGEREMOTEID((byte)0x27,ChangeRemoteId.class),
    RECORD_TYPE_TEMPBASALRATE((byte)0x33,TempBasalRate.class),
    RECORD_TYPE_LOWRESERVOIR((byte)0x34,LowReservoir.class),
    RECORD_TYPE_IAN3F((byte)0x3F,Ian3F.class),
    RECORD_TYPE_BOLUSWIZARDCHANGE((byte)0x5A,BolusWizardChange.class),
    RECORD_TYPE_BOLUSWIZARD((byte)0x5B,BolusWizard.class),
    RECORD_TYPE_UNABSORBEDINSULIN((byte)0x5C,UnabsorbedInsulin.class),
    RECORD_TYPE_OLD6C((byte)0x6C,Old6c.class),
    RECORD_TYPE_RESULTTOTALS((byte)0x6D,ResultTotals.class),
    RECORD_TYPE_SARA6E((byte)0x6E,Sara6E.class),
    RECORD_TYPE_CHANGEUTILITY((byte)0x63,ChangeUtility.class),
    RECORD_TYPE_CHANGETIMEDISPLAY((byte)0x64,ChangeTimeDisplay.class),
    RECORD_TYPE_BASALPROFILESTART((byte)0x7B,BasalProfileStart.class);

    private byte opcode;
    private Class mRecordClass;

    public byte opcode() {
        return opcode;
    }
    public Class recordClass() {
        return mRecordClass;
    }
    RecordTypeEnum(byte b,Class c) {
        opcode = b;
        mRecordClass = c;
    }
    public static RecordTypeEnum fromByte(byte b) {
        for(RecordTypeEnum en : RecordTypeEnum.values()) {
            if (en.opcode() == b) {
                return en;
            }
        }
        return RECORD_TYPE_NULL;
    }

    private static final String TAG = "RecordTypeEnum";
    public <T extends Record> T getRecordClass() {
        Constructor<T> ctor;
        T record = null;
        try {
            Class c = recordClass();
            if (c!=null) {
                ctor = recordClass().getConstructor();
                if (ctor != null) {
                    record = ctor.newInstance();
                }
            }
        } catch (NoSuchMethodException e) {
            // NOTE: these were all OR'd together, but android requires us to separate them.
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return record;
    }
}
