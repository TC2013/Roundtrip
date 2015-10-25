package com.gxwtech.RileyLink;

import android.bluetooth.BluetoothDevice;

import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.GattAttributes;
import com.gxwtech.rtdemo.bluetooth.GattCharacteristicReadCallback;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicReadOperation;

import java.util.UUID;


/**
 * Created by geoff on 8/13/15.
 */
public class ReadBatteryLevelCommand implements RileyLinkCommand {
    protected byte[] readbuffer;
    public ReadBatteryLevelCommand() {
    }
    public RileyLinkCommandResult run(RileyLink rl, int timeout_millis) {
        RileyLinkCommandResult rval;
        boolean sentOK = rl.readBatteryLevel(
                new GattCharacteristicReadCallback() {
                    @Override
                    public void call(byte[] characteristic) {
                        // handle data here
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
