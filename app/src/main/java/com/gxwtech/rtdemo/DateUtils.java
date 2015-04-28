package com.gxwtech.rtdemo;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Created by geoff on 4/16/15.
 */
public class DateUtils {
    public static Calendar makeCalendar(long milliseconds) {
        // get the supported ids for GMT-06:00 (Central Standard Time)
        String[] ids = TimeZone.getAvailableIDs(-6 * 60 * 60 * 1000);
        // if no ids were returned, something is wrong. get out.
        if (ids.length == 0)
            System.exit(0);

        // begin output
        System.out.println("Current Time");

        // create a Central Standard Time time zone
        SimpleTimeZone pdt = new SimpleTimeZone(-6 * 60 * 60 * 1000, ids[0]);

        // set up rules for Daylight Saving Time
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);

        // create a GregorianCalendar with the Central Daylight time zone
        // and the current date and time
        Calendar calendar = new GregorianCalendar(pdt);
        calendar.setTime(new Date(milliseconds));
        return calendar;
    }

    public static String HHMMSS(Calendar c) {
        return HHMMSS(c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
    }

    public static String HHMMSS(int HH, int MM, int SS) {
        return String.format("%02d:%02d:%02d", HH, MM, SS);
    }


}
