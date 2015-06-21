package com.gxwtech.rtdemo.Carelink;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by fokko on 20-6-15.
 */
public class CarelinkUsbDevice {
    private static final String TAG = "CarelinkUsbDevice";

    private static CarelinkUsbDevice instance = null;
    private final UsbDevice carelinkDevice;

    public static UsbDevice getDevice(Context ctx) {
        if (instance == null) {
            instance = new CarelinkUsbDevice(ctx);
            Log.e(TAG, "Re-using existing carelink device");
        }
        return instance.carelinkDevice;
    }

    private CarelinkUsbDevice(Context ctx) {
        UsbManager manager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);

        // else, try to go get it.
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        UsbDevice device = null;
        while(deviceIterator.hasNext()){
            device = deviceIterator.next();
            if (deviceIsCarelink(device)) {
                break;
            } else {
                device = null;
            }
        }

        carelinkDevice = device;
        if (carelinkDevice == null) {
            Log.e(TAG,"Failed to find suitable carelink device");
        } else {
            Log.e(TAG,"Found new carelink device");
        }
    }

    // Magic numbers for Carelink stick
    private static final int CareLinkVendorId = 2593;
    private static final int CareLinkProductId = 32769;
    private static boolean deviceIsCarelink(UsbDevice device) {
        if (device == null)
            return false;
        return ((device.getVendorId() == CareLinkVendorId) && (device.getProductId() == CareLinkProductId));
    }
}
