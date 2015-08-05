package com.gxwtech.rtdemo.carelink.util;

import java.util.ArrayList;

/**
 * Created by geoff on 4/28/15.
 */
public class StringUtil {

    // these should go in some project-wide string utils package
    public static String join(ArrayList<String> ra, String joiner) {
        int sz = ra.size();
        String rval = "";
        int n;
        for (n = 0; n < sz; n++) {
            rval = rval + ra.get(n);
            if (n < sz - 1) {
                rval = rval + joiner;
            }
        }
        return rval;
    }

    public static String testJoin() {
        ArrayList<String> ra = new ArrayList<String>();
        ra.add("one");
        ra.add("two");
        ra.add("three");
        return join(ra, "+");
    }

}
