package com.gxwtech.rtdemo.Medtronic;

import android.util.Log;

import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfile;
import com.gxwtech.rtdemo.Medtronic.PumpData.BasalProfileTypeEnum;

/**
 * Created by geoff on 5/5/15.
 * There are three profiles: Standard (STD), A and B.  They are the same format, different data.
 *
 */
public class ReadProfileCommand extends MedtronicCommand {
  private static final String TAG = "ReadProfileCommand";
  protected BasalProfile mProfile;
  public ReadProfileCommand() {
    mProfile = new BasalProfile();
    init(MedtronicCommandEnum.CMD_M_READ_STD_PROFILES);
  }
  protected void parse(byte[] receivedData) {
    if (receivedData == null) {
      Log.e(TAG,"Passed null data ?!");
    } else {
      mProfile.setRawData(receivedData);
    }
  }
  public BasalProfile getProfile() {
    return mProfile;
  }
  public boolean setProfileType(BasalProfileTypeEnum which) {
    switch(which) {
      case STD: init(MedtronicCommandEnum.CMD_M_READ_STD_PROFILES);
        return true;
      case A: init(MedtronicCommandEnum.CMD_M_READ_A_PROFILES);
        return true;
      case B: init(MedtronicCommandEnum.CMD_M_READ_B_PROFILES);
        return true;
      default: return false;
    }
  }
}

/* from decocare:

# MMPump512/    CMD_READ_STD_PROFILES   146     0x92    ('\x92')        ??
class ReadProfile_STD512 (PumpCommand):
  """
    >>> import json
    >>> schedule = ReadProfile_STD512.decode(ReadProfile_STD512._test_result_1)
    >>> len(schedule)
    4
    >>> print json.dumps(schedule[0])
    {"start": "00:00:00", "rate": 0.8}
    >>> print json.dumps(schedule[1])
    {"start": "06:30:00", "rate": 0.9500000000000001}
    >>> print json.dumps(schedule[2])
    {"start": "09:30:00", "rate": 1.1}
    >>> print json.dumps(schedule[3])
    {"start": "14:00:00", "rate": 0.9500000000000001}

  """
  _test_result_1 = bytearray([
    32, 0, 0,
    38, 0, 13,
    44, 0, 19,
    38, 0, 28,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0
  ])
  _test_schedule = {'total': 22.50, 'schedule': [
    { 'start': '12:00A', 'rate': 0.80 },
    { 'start': '6:30A', 'rate': 0.95 },
    { 'start': '9:30A', 'rate': 1.10 },
    { 'start': '2:00P', 'rate': 0.95 },
  ]}
  code = 146
  @staticmethod
  def decode (data):
    i = 0
    schedule = [ ]
    end = [ 0, 0, 0 ]
    none = [ 0, 0, 0x3F ]
    for i in xrange(len(data)/3):
      off = i*3
      r, z, m = data[off : off + 3]
      if [r,z,m] in [end, none]:
        break
      schedule.append(dict(start=str(lib.basal_time(m)), rate=r*.025))
    return schedule
  def getData (self):
    return self.decode(self.data)

 */