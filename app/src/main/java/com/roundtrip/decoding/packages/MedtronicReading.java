package com.roundtrip.decoding.packages;

import java.util.Date;

public abstract class MedtronicReading {
    public static final int EXPIRATION_FOUR_MINUTES = 60 * 4;
    public static final int EXPIRATION_TWO_AND_HALF_MINUTES = 60 * 2 + 30;
    public static final int EXPIRATION_MINUTE = 60;
    public static final int EXPIRATION_FIVE_MINUTES = 5 * 60;

    private Date created;

    public MedtronicReading(byte[] data) throws ParseException {
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
        return createdDifference(medtronicReading.getCreated());
    }

    public long createdDifference(Date targetDate) {
        long t1 = this.created.getTime();
        long t2 = targetDate.getTime();

        return Math.abs(t1 - t2);
    }
}