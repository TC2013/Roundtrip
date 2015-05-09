package com.gxwtech.rtdemo.Services.PumpManager;

import android.os.Parcel;
import android.os.Parcelable;

import com.gxwtech.rtdemo.Medtronic.PumpData.PumpSettings;

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

    @Override
    public int describeContents() {
        return 0;
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