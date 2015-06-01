package com.gxwtech.rtdemo;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * Created by geoff on 5/29/15.
 */
public class BGReadingParcel extends BGReading implements Parcelable {
    public BGReadingParcel() { init(new DateTime(0),0.0); }

    public void init(DateTime dt, double bg) {
        super.init(dt,bg);
    }

    // copy constructor
    public BGReadingParcel(BGReadingParcel parcel) {
        init(parcel.mTimestamp,parcel.mBg);
    }

    public BGReadingParcel(BGReading bgr) {
        init(bgr.mTimestamp, bgr.mBg);
    }

    @Override
    public int describeContents() {
        return 0; // Unneeded.  Could be needed if we needed hints about our internal structure.
    }

    public String[] getContentsAsStringArray() {
        ArrayList<String> ra = new ArrayList<>();
        ra.add(mTimestamp.toString()); // fixme: formatting?
        ra.add(String.format("%d mg/dL",((int)mBg)));
        String[] rval = new String[ra.size()];
        // toArray will allocate space if necessary.
        rval = ra.toArray(rval);
        return rval;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTimestamp.toString()); // todo: proper formatting, for re-reading?
        out.writeDouble(mBg);
    }

    public static final Parcelable.Creator<BGReadingParcel> CREATOR
            = new Parcelable.Creator<BGReadingParcel>() {
        public BGReadingParcel createFromParcel(Parcel in) {
            return new BGReadingParcel(in);
        }

        public BGReadingParcel[] newArray(int size) {
            return new BGReadingParcel[size];
        }
    };

    private BGReadingParcel(Parcel in) {
        String datestring = in.readString();
        double bg = in.readDouble();
        DateTime dt = new DateTime();
        dt.parse(datestring);
        init(dt,bg);
    }
}
