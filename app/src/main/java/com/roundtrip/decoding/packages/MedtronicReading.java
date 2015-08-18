package com.roundtrip.decoding.packages;

import java.util.Date;

public abstract class MedtronicReading {
    public static final int EXPIRATION_FOUR_MINUTES = 60*4;

    Date created;

    public MedtronicReading(byte[] data) throws ParseException{
        this.created = new Date();
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    protected abstract int packageLength();

    // In seconds
    public long createdDifference(MedtronicReading medtronicReading) {
        long t1 = this.created.getTime();
        long t2 = medtronicReading.getCreated().getTime();

        return Math.abs(t1 - t2);
    }
}