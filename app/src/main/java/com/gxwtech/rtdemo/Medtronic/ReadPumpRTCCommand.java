package com.gxwtech.rtdemo.Medtronic;

import org.joda.time.DateTime;

/**
 * Created by geoff on 5/5/15.
 */
public class ReadPumpRTCCommand extends MedtronicCommand {
    private static final String TAG = "ReadPumpRTCCommand";
    DateTime mTimestamp;
    public ReadPumpRTCCommand() {
        init(MedtronicCommandEnum.CMD_M_READ_RTC);
        mTimestamp = new DateTime(0); // a clearly wrong date, to initialize.
    }

    protected void parse(byte[] receivedData) {
        if (receivedData == null) {
            return;
        }
        if (receivedData.length < 7) {
            return;
        }
        //Log.d(TAG,"Raw data: " + HexDump.dumpHexString(receivedData));
        /*
        at offset 0: 0B 0F 23 is our time-of-day
        at offset 4: DF 06 04 is our date
        */
        int hours = receivedData[0];
        int minutes = receivedData[1];
        int seconds = receivedData[2];
        int year = (receivedData[4] & 0x0f) + 2000;
        int month = receivedData[5];
        int day = receivedData[6];

        /*
        Log.d(TAG,String.format("Raw pump bytes report: %04d-%02d-%02d %02d:%02d:%02d",
                        year,month,day,hours,minutes,seconds));
        */
        mTimestamp = new DateTime().withDate(year,month,day).withTime(hours,minutes,seconds,0);
        //Log.d(TAG,"DateTime reports " + mTimestamp.toDateTimeISO().toString());
    }

    public DateTime getRTCTimestamp() {
        return mTimestamp;
    }
}



/* Decocare gives this, as final json output:
{
  "_type": "RTC",
  "observed_at": "2015-04-24T00:11:40.253013-05:00",
  "model": "722",
  "now": "2015-04-24T00:10:05-05:00"
}

Decocare raw input from pump:

0000   0x02 0x00 0x03 0x00 0xcb 0x80 0x40 0xa7    ......@.
0008   0x01 0x46 0x73 0x24 0x2e 0x00 0x0a 0x05    .Fs$....
0010   0x07 0xdf 0x04 0x18 0x00 0x00 0x00 0x00    ........
0018   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
0020   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
0028   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
0030   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
0038   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
0040   0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00    ........
0048   0x00 0x00 0x00 0x00 0x00 0x04              ......

At offset 13: 0x00 0x0a 0x05 is our time-of-day
At offset 17: 0xdf 0x04 0x18 is our date

This is a sample of receivedData, as seen by ReadPumpRTCCommand:

    0x00000000 0B 0F 23 07 DF 06 04 00 00 00 00 00 00 00 00 00 ..#.............
    0x00000010 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................
    0x00000030 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

at offset 0: 0B 0F 23 is our time-of-day
at offset 4: DF 06 04 is our date

*/
