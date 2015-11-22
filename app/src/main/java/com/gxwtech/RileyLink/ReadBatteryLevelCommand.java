package com.gxwtech.RileyLink;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.GattAttributes;
import com.gxwtech.rtdemo.bluetooth.GattCharacteristicReadCallback;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicReadOperation;
import com.gxwtech.rtdemo.services.RoundtripService;

import java.util.UUID;


/**
 * Created by geoff on 8/13/15.
 */
public class ReadBatteryLevelCommand implements RileyLinkCommand {
    Context mContext;
    protected byte[] readbuffer;

    public ReadBatteryLevelCommand(Context context) {
        mContext = context;
    }
    public RileyLinkCommandResult run(RileyLink rl, int timeout_millis) {
        RileyLinkCommandResult rval;
        boolean sentOK = rl.readBatteryLevel(
                new GattCharacteristicReadCallback() {
                    @Override
                    public void call(byte[] characteristic) {
                        mContext.startService(new Intent(mContext.getApplicationContext(), RoundtripService.class)
                                .setAction(Intents.RILEYLINK_BATTERY)
                                .putExtra("srq",Constants.SRQ.BATTERY_LEVEL_REPORT)
                                .putExtra("chara",characteristic));
                    }
                }
        );
        if (sentOK) {
            rval = new RileyLinkCommandResult(readbuffer,RileyLinkCommandResult.STATUS_OK,"Sent OK");
        } else {
            rval = new RileyLinkCommandResult(readbuffer,RileyLinkCommandResult.STATUS_ERROR,"RileyLink reports error queuing read battery level command");
        }
        return rval;
    }
}
