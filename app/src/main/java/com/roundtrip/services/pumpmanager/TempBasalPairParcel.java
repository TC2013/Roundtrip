package com.roundtrip.services.pumpmanager;

import android.os.Parcel;
import android.os.Parcelable;

import com.roundtrip.medtronic.PumpData.TempBasalPair;

import java.util.ArrayList;

public class TempBasalPairParcel extends TempBasalPair implements Parcelable {
    public static final Parcelable.Creator<TempBasalPairParcel> CREATOR
            = new Parcelable.Creator<TempBasalPairParcel>() {
        public TempBasalPairParcel createFromParcel(Parcel in) {
            return new TempBasalPairParcel(in);
        }

        public TempBasalPairParcel[] newArray(int size) {
            return new TempBasalPairParcel[size];
        }
    };

    public TempBasalPairParcel() {
        init(0.0, 0);
    }

    // copy constructor
    public TempBasalPairParcel(TempBasalPairParcel parcel) {
        init(parcel.mInsulinRate, parcel.mDurationMinutes);
    }

    public TempBasalPairParcel(TempBasalPair pair) {
        init(pair.mInsulinRate, pair.mDurationMinutes);
    }

    private TempBasalPairParcel(Parcel in) {
        init(in.readDouble(), in.readInt());
    }

    public void init(double insulinRate, int durationMinutes) {
        mInsulinRate = insulinRate;
        mDurationMinutes = durationMinutes;
    }

    @Override
    public int describeContents() {
        return 0; // Unneeded.  Could be needed if we needed hints about our internal structure.
    }

    public String[] getContentsAsStringArray() {
        ArrayList<String> ra = new ArrayList<>();
        ra.add(String.format("Insulin Rate: %.3fU", mInsulinRate));
        ra.add(String.format("Duration: %d minutes", mDurationMinutes));
        String[] rval = new String[ra.size()];
        // toArray will allocate space if necessary.
        rval = ra.toArray(rval);
        return rval;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(mInsulinRate);
        out.writeInt(mDurationMinutes);
    }
}
