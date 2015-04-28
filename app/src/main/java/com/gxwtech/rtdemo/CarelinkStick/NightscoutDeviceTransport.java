package com.gxwtech.rtdemo.CarelinkStick;

import android.hardware.usb.UsbDeviceConnection;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

/**
 * Created by geoff on 4/22/15.
 */
public class NightscoutDeviceTransport implements DeviceTransport {
    public static final int mMaxPacketSize = 64;
    public static final int writeTimeout_millis = 10;
    public static final int readTimeout_millis = 10;
    protected UsbSerialDriver mDriver = null;
    protected UsbDeviceConnection mDeviceConnection = null;
    boolean mIsOpen = false;

    public NightscoutDeviceTransport(UsbSerialDriver driver, UsbDeviceConnection connection) {
        mDriver = driver;
        mDeviceConnection = connection;
        mIsOpen = false;
    }

    public boolean isReady() { return ((mDriver != null) && (mDeviceConnection != null)); }
    public boolean isOpen() { return mIsOpen; }

    protected UsbSerialPort getPort() throws IOException {
	if (!isReady()) {
	    throw new java.io.IOException("Carelink Transport not ready.");
	}
	return mDriver.getPorts().get(0);
    }

    /**
     * Opens and initializes the device as a USB serial device. Upon success,
     * caller must ensure that {@link #close()} is eventually called.
     *
     * @throws java.io.IOException on error opening or initializing the device.
     */
    public void open() throws IOException {
        if (!isReady()) {
            throw new java.io.IOException("Carelink Transport not ready.");
        }
        getPort().open(mDeviceConnection);
        mIsOpen = true;
    }

    /**
     * Closes the serial device.
     *
     * @throws java.io.IOException on error closing the device.
     */
    public void close() throws IOException {
        if (!isReady()) {
            throw new java.io.IOException("Carelink Transport not ready.");
        }
        getPort().close();
        mIsOpen = false;
    }

    /**
     * Reads as many bytes as possible into the destination buffer.
     *
     * @param dest          the destination byte buffer
     * @param timeoutMillis the timeout for reading
     * @return the actual number of bytes read
     * @throws java.io.IOException if an error occurred during reading
     */
    public int read(final byte[] dest, final int timeoutMillis) throws IOException {
	if (!isReady()) {
	    throw new java.io.IOException("Carelink Transport not ready.");
	}
	int rval = getPort().read(dest,timeoutMillis);
        return rval;
    }

    /**
     * Reads as many bytes as possible into the destination buffer.
     *
     * @param size          size to read
     * @param timeoutMillis the timeout for reading
     * @return the actual number of bytes read
     * @throws java.io.IOException if an error occurred during reading
     */
    public byte[] read(int size, final int timeoutMillis) throws IOException {
        byte dest[] = null;
        if (!isReady()) {
            throw new java.io.IOException("Carelink Transport not ready.");
        }
        if (!isOpen()) {
            throw new IOException("FAIL: Read before open!)");
        }
        if (size > mMaxPacketSize) {
            dest = new byte[mMaxPacketSize];
        } else {
            dest = new byte[size];
        }
        int nRead = read(dest,timeoutMillis); // call the other read function
        return dest;
    }

    /**
     * Writes as many bytes as possible from the source buffer.
     *
     * @param src           the source byte buffer
     * @param timeoutMillis the timeout for writing
     * @return the actual number of bytes written
     * @throws java.io.IOException if an error occurred during writing
     */
    public int write(final byte[] src, final int timeoutMillis) throws IOException {
	int rval = 0;
        if (!isReady()) {
            throw new java.io.IOException("Carelink Transport not ready.");
        }
        if (!isOpen()) {
            throw new IOException("FAIL: Write before open!)");
        }
        getPort().write(src,timeoutMillis);
        // check for how many bytes were actually written?
        rval = src.length;
        return rval;
    }

    public boolean isConnected(int vendorId, int productId) {
        return isReady();
    }


}
