package com.gxwtech.rtdemo.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.gxwtech.rtdemo.HexDump;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by mark on 14/09/14.
 */
public class CareLinkUsb {

    //CareLink USB Device (ProductID: 32769, VendorID: 2593)
    //MMCommander (ProductID: 5798, VendorID: 1105)
    //Contour Link USB Device (ProductID: 25344, VendorID: 6777)

    public static final int CARELINK_PRODUCT_ID = 32769;
    public static final int CARELINK_VENDOR_ID = 2593;
    private static final int MAX_PACKAGE_SIZE = 64;

    private static final String TAG = "CareLinkUsb";
    private static final boolean DEBUG_CARELINKUSB = false;

    // need context to get at usb manager
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mInterface;
    private UsbEndpoint epIN, epOUT;


    public CareLinkUsb() {
    }

    public void open(Context context) throws UsbException {
        if (DEBUG_CARELINKUSB) {
            Log.v(TAG, "CarelinkUSB open()");
        }
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (DEBUG_CARELINKUSB) {
            Log.v(TAG, "Enumerating connected devices...");
        }
        // Getting the CareLink UsbDevice object
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            mUsbDevice = deviceIterator.next();
            if (mUsbDevice.getVendorId() == CARELINK_VENDOR_ID && mUsbDevice.getProductId() == CARELINK_PRODUCT_ID) {
                break;
            }
        }

        if (mUsbDevice == null) {
            throw new UsbException("Device not found");
        }

        // Assigning interface
        mInterface = mUsbDevice.getInterface(0);

        // Lovely...  We "get a device" when the carelink is unplugged...
        // Assigning endpoint in and out
        try {
            epOUT = mInterface.getEndpoint(0);
            epIN = mInterface.getEndpoint(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            // This means there's no Carelink attached :(
            throw new UsbException("Device not attached");
        }

        // Open connection
        mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);

        if (mUsbDeviceConnection == null) {
            throw new UsbException("no connection available");
        }
    }

    public void close() throws UsbException {
        Log.v(TAG,"CarelinkUSB close()");
        if (mUsbDeviceConnection == null) {
            throw new UsbException("no connection available");
        }
        mUsbDeviceConnection.releaseInterface(mInterface);
        mInterface = null;
        mUsbDeviceConnection.close();
        mUsbDeviceConnection = null;
    }

    public void drainQueue() throws UsbException {
        byte[] buf = new byte[MAX_PACKAGE_SIZE];
        boolean done = false;
        if (mUsbDeviceConnection == null) {
            throw new UsbException("no connection available");
        }
        // use bulkTransfer to drain queue
        while (!done) {
            int nRead = mUsbDeviceConnection.bulkTransfer(epIN, buf, MAX_PACKAGE_SIZE, 100);
            if (nRead < 0) {
                throw new UsbException(String.format("Error in drainQueue, bulkTransfer returned %d", nRead));
            } else if (nRead == 0) {
                done = true;
                if (DEBUG_CARELINKUSB) {
                    Log.v("drainQueue", String.format("read 0 bytes, queue drained.", nRead));
                }
            } else {
                if (DEBUG_CARELINKUSB) {
                    Log.v("drainQueue", String.format("read %d bytes, trying to drain more.", nRead));
                    Log.v("drainQueue", "Dump of drained bytes: " + HexDump.dumpHexString(buf));
                }
            }
        }
    }

    /**
     * Gets the response from the connected device.
     * @param outRequest UsbRequest referencing the request.
     * @return Returns the response in a byte[].
     * @throws UsbException
     */
    public byte[] read(UsbRequest outRequest,int readSize) throws UsbException {
        if (mUsbDeviceConnection == null) {
            throw new UsbException("no connection available");
        }

        boolean enqueued = (boolean)outRequest.getClientData();
        if (!enqueued) {
            throw new UsbException("Original request was not enqueued -- cannot receive.");
        }

        if (readSize < 64) { readSize = 64; }

        //ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKAGE_SIZE);
        ByteBuffer buffer = ByteBuffer.allocate(readSize);


        // Receive data from device
        if (outRequest.equals(mUsbDeviceConnection.requestWait())) {
            UsbRequest inRequest = new UsbRequest();
            inRequest.initialize(mUsbDeviceConnection, epIN);
            if (inRequest.queue(buffer, readSize)) {
                mUsbDeviceConnection.requestWait();
                return buffer.array();
            }
        }
        return null;
    }

    /**
     * Write a command to the connected device.
     * @param command Byte[] containing the opcode for the command.
     * @return Returns the response in a byte[].
     * @throws UsbException
     */
    public UsbRequest write(byte[] command) throws UsbException {
        if (mUsbDeviceConnection == null) {
            throw new UsbException("no connection available");
        }

        ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKAGE_SIZE);

        UsbRequest outRequest = new UsbRequest();
        outRequest.initialize(mUsbDeviceConnection, epOUT);

        buffer.put(command);
        boolean queueSuccess = outRequest.queue(buffer, MAX_PACKAGE_SIZE);
        outRequest.setClientData(queueSuccess);
        return outRequest;
    }

    /**
     * http://stackoverflow.com/questions/12345953/android-usb-host-asynchronous-interrupt-transfer
     *
     * Wrapper for CareLinkUsb.write() and CareLinkUsb.read()
     * @param command Byte[] containing the opcode for the command.
     * @return Returns a reference to the UsbRequest put in the output queue.
     * @throws UsbException
     */

    public byte[] sendCommand(byte[] command, int delayMillis, int readSize)  throws UsbException {
//        if (mUsbDeviceConnection == null) {
//            throw new UsbException("no connection available");
//        }
//
//        // MaxPacketSize = 64
//        int bufferMaxLength = epOUT.getMaxPacketSize();
//        ByteBuffer buffer = ByteBuffer.allocate(bufferMaxLength);
//        UsbRequest outRequest = new UsbRequest();
//        outRequest.initialize(mUsbDeviceConnection, epOUT);
//
////        for(int i = 0; i < command.length; i++) {
////            buffer.put(command[i]);
////
////        }
//
////        buffer.put(command, 0, command.length);
//
//        // Putting op in buffer
//        buffer.put(command);
//        // Queue outbound request
//        outRequest.queue(buffer, bufferMaxLength);
//
//        // Receive data from device
//        if (outRequest.equals(mUsbDeviceConnection.requestWait())) {
//            UsbRequest inRequest = new UsbRequest();
//            inRequest.initialize(mUsbDeviceConnection, epIN);
//            if (inRequest.queue(buffer, bufferMaxLength)) {
//                mUsbDeviceConnection.requestWait();
//                return buffer.array();
//            }
//        }
//        return null;
        UsbRequest request = write(command);
        if ((boolean)request.getClientData() == false) {
            Log.e(TAG, "Error writing USB data");
            request.close();
            return null;
        }
        if (delayMillis > 0) {
            try {
                if (DEBUG_CARELINKUSB) {
                    Log.v(TAG, String.format("Sleeping %d milliseconds.", delayMillis));
                }
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Log.i(TAG, "Sleep Interrupted: " + e.getMessage());
            }
        }
        byte[] rval = read(request,readSize);
        if ((boolean)request.getClientData() == false) {
            Log.v(TAG, "Error reading USB data");
        }
        request.close();
        return rval;
    }

    public byte[] sendCommand(byte[] command) throws UsbException {
        return sendCommand(command,0,MAX_PACKAGE_SIZE);
    }

    // Use this function if your request will send back multiple 64 byte frames.
    // delayMillis delays between the write and the read
    public byte[] sendCommandReceiveMultiple(byte[] command, int delayMillis, int fullSize)
            throws UsbException {
        int recordsNeeded = fullSize / MAX_PACKAGE_SIZE;
        if ((fullSize % MAX_PACKAGE_SIZE) > 0) {
            recordsNeeded++;
        }
        Log.v("CareLinkUsb",String.format("Expecting to receive %d bytes in %d frames",fullSize,recordsNeeded));
        byte[] fullResponse = new byte[0];
        UsbRequest request = write(command);
        if (delayMillis > 0) {
            try {
                Log.v(TAG, String.format("Sleeping %d milliseconds.", delayMillis));
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Log.i(TAG, "Sleep Interrupted: " + e.getMessage());
            }
        }
        fullResponse = read(request, fullSize);

        Log.v(TAG,String.format("Received full response in %d frames:",recordsNeeded)
                + HexDump.dumpHexString(fullResponse));
        return fullResponse;
    }
}
