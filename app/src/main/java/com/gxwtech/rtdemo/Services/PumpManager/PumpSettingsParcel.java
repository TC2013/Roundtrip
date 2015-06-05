package com.gxwtech.rtdemo.Services.PumpManager;

import android.os.Parcel;
import android.os.Parcelable;

import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfileTypeEnum;
import com.gxwtech.rtdemo.Medtronic.PumpData.PumpSettings;

import java.util.ArrayList;

/**
 * Created by geoff on 5/9/15.
 *
 * See header notes for PumpSettings.  Also, note this:
 *
 * https://developer.android.com/reference/android/os/Parcel.html
 *
 * "Parcel is *not* a general-purpose serialization mechanism. This class
 * (and the corresponding Parcelable API for placing arbitrary objects
 * into a Parcel) is designed as a high-performance IPC transport.
 * As such, it is not appropriate to place any Parcel data in to persistent
 * storage: changes in the underlying implementation of any of the data in
 * the Parcel can render older data unreadable."
 *
 * This is intended to be passed between the background (RTDemoService) thread
 * and the foreground UI thread(s).
 */
public class PumpSettingsParcel extends PumpSettings implements Parcelable {

    public PumpSettingsParcel() { }

    public boolean initFromPumpSettings(PumpSettings p) {
        return parseFrom(p.getRawData());
    }

    // copy constructor
    public PumpSettingsParcel(PumpSettingsParcel p) {
        //lazy...
        initFromPumpSettings(p);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String[] getContentsAsStringArray() {
        ArrayList<String> ra = new ArrayList<>();

        ra.add(String.format("Auto Off Duration (hours): %d", mAutoOffDuration_hours));
        ra.add(String.format("Alarm Mode: %d", mAlarmMode));
        ra.add(String.format("Alarm Volume: %d", mAlarmVolume));
        ra.add(String.format("Audio Bolus Enable: %s", mAudioBolusEnable ? "true" : "false"));
        ra.add(String.format("Audio Bolus Size: %.2f", mAudioBolusSize));
        ra.add(String.format("Variable Bolus Enable: %s", mVariableBolusEnable ? "true" : "false"));
        ra.add(String.format("Max Bolus Size(U): %.2f", mMaxBolus));
        ra.add(String.format("Max Basal Rate (U/h): %.2f",mMaxBasal));
        ra.add(String.format("Time Format: %d", mTimeFormat));
        ra.add(String.format("Insulin Concentration(%%): %s", (mInsulinConcentration == 0) ? "50" : "100"));
        ra.add(String.format("Patterns Enabled: %s",mPatternsEnabled ? "true":"false"));
        String id;
        switch (mSelectedPattern) {
            case STD: id = "STD";
            break;
            case A: id = "A";
            break;
            case B: id = "B";
            break;
            default: id = "???";
        }
        ra.add(String.format("Selected Pattern: %s", id));
        ra.add(String.format("RF Enable: %s", mRFEnable ? "true" : "false"));
        ra.add(String.format("Block Enable: %s", mBlockEnable ? "true" : "false"));
        ra.add(String.format("Temp Basal Type: %d", mTempBasalType));
        ra.add(String.format("Temp Basal Rate: %d", mTempBasalRate));
        ra.add(String.format("Paradigm Enable: %d", mParadigmEnable));
        ra.add(String.format("Insulin Action Type: %d", mInsulinActionType));
        ra.add(String.format("Low Reservoir Warn Type: %d", mLowReservoirWarnType));
        ra.add(String.format("Low Reservoir Warn Point: %d", mLowReservoirWarnPoint));
        ra.add(String.format("Keypad Lock Status: %d", mKeypadLockStatus));

        String[] rval = new String[ra.size()];
        // toArray will allocate space if necessary.
        rval = ra.toArray(rval);

        return rval;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByteArray(mRawData);
    }

    public static final Parcelable.Creator<PumpSettingsParcel> CREATOR
            = new Parcelable.Creator<PumpSettingsParcel>() {
        public PumpSettingsParcel createFromParcel(Parcel in) {
            return new PumpSettingsParcel(in);
        }

        public PumpSettingsParcel[] newArray(int size) {
            return new PumpSettingsParcel[size];
        }
    };

    private PumpSettingsParcel(Parcel in) {
        byte[] data = new byte[MAXIMUM_DATA_LENGTH];
        in.readByteArray(data);
        parseFrom(data);
    }
}