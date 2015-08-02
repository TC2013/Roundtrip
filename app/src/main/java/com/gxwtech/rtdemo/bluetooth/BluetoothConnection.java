package com.gxwtech.rtdemo.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.gxwtech.rtdemo.Constants;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Fokko on 2-8-15.
 */
public class BluetoothConnection {
    private static final String LS =System.getProperty("line.separator");
    private static final String TAG = "BluetoothConnection";

    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice bluetoothDeviceRileyLink = null;
    private BluetoothGatt bluetoothConnectionGatt = null;

    private final Context context;

    private static BluetoothConnection instance = null;

    protected BluetoothConnection() {
        this.context = null;
    }

    protected BluetoothConnection(Context context) {
        this.context = context;
    }

    public static BluetoothConnection getInstance(Context context) {
        if (instance == null) {
            synchronized (BluetoothConnection.class) {
                if (instance == null) {
                    instance = new BluetoothConnection(context);
                }
            }
        }
        return instance;
    }

    public String connect() {

        String message;

        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        if (this.bluetoothAdapter != null) {
            if (this.bluetoothAdapter.isEnabled()) {
                final Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

                this.bluetoothDeviceRileyLink = null;
                for (BluetoothDevice device : devices) {
                    if (device.getName().equals(Constants.PrefName.Bluetooth_RileyLink_Name)) {
                        this.bluetoothDeviceRileyLink = device;
                    }
                }

                if (this.bluetoothDeviceRileyLink != null) {
                    message = "RileyLink has been found.";

                    //TODO: https://github.com/suzp1984/Light_BLE/blob/master/Light_BLE/ble/src/main/java/org/zpcat/ble/BluetoothLeService.java#L285
                    // Connect using Gatt, any further communication will be done using asynchronous calls.
                    this.bluetoothConnectionGatt = this.bluetoothDeviceRileyLink.connectGatt(context, true, mGattcallback);
                    Log.w(TAG, "RileyLink has been found, staring to establish connection: " + this.bluetoothConnectionGatt);
                } else {
                    Log.w(TAG, "Could not find RileyLink.");
                    message = "Could not find RileyLink.";
                }

            } else {
                message = "Bluetooth is not enabled on device.";
            }
        } else {
            message = "Bluetooth is not available on device.";
        }

        return message;
    }

    public void sendCommand(byte[] data) {
        Log.d(TAG, "Sending packet");

        final byte[] minimedRFData = RileyLinkUtil.composeRFStream(data);

        if (this.bluetoothConnectionGatt == null) {
            Log.e(TAG, "GATT connection not available!");
            return;
        }

        final BluetoothGattService service = this.bluetoothConnectionGatt.
                getService(UUID.fromString(GattAttributes.GLUCOSELINK_SERVICE_UUID));

        if (service == null) {
            Log.e(TAG, "Service not found!");
            return;
        }

        final BluetoothGattCharacteristic characteristic = service.
                getCharacteristic(UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT));

        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found!");
            return;
        }

        characteristic.setValue(minimedRFData);
        bluetoothConnectionGatt.writeCharacteristic(characteristic);

        Log.d(TAG, "Characteristic is being send.");
    }

    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.w(TAG, String.format("onCharacteristicChanged " + characteristic));


        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, String.format("onCharacteristicRead " + characteristic + " status " + status));


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, String.format("onCharacteristicWrite " + characteristic + " status " + status));


        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            final String statusMessage;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                statusMessage = "SUCCESS";
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                statusMessage = "FAILED";
            } else {
                statusMessage = "UNKNOWN (" + status + ")";
            }

            final String stateMessage;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                stateMessage = "CONNECTED";
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                stateMessage = "CONNECTING";
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                stateMessage = "DISCONNECTED";
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                stateMessage = "DISCONNECTING";
            } else {
                stateMessage = "UNKOWN (" + newState + ")";
            }

            Log.w(TAG, String.format("onConnectionStateChange " + statusMessage + " " + stateMessage));


            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                if (gatt.discoverServices()) {
                    Log.w(TAG, "Starting to discover GATT Services.");
                } else {
                    Log.w(TAG, "Cannot discover GATT Services.");
                }
            }

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.w(TAG, String.format("onDescriptorRead " + descriptor + " status " + status));


        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.w(TAG, String.format("onDescriptorWrite " + descriptor + " status " + status));


        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.w(TAG, String.format("onMtuChanged " + mtu + " status " + status));


        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.w(TAG, String.format("onReadRemoteRssi " + rssi + " status " + status));


        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.w(TAG, String.format("onReliableWriteCompleted status " + status));


        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final String message;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    final String uuidServiceString = service.getUuid().toString();

                    String debugString = "Found service: " + GattAttributes.lookup(uuidServiceString, "Unknown device") + " (" + uuidServiceString + ")" + LS;
                    for (BluetoothGattCharacteristic character : characteristics) {
                        final String uuidCharacteristicString = character.getUuid().toString();
                        debugString += "    - " + GattAttributes.lookup(uuidCharacteristicString, "Unknown device") + " (" + uuidCharacteristicString + ")" + LS;
                    }
                    Log.w(TAG, "" + debugString);
                }

                message = "Got response, found: " + services.size() + " so far.";
            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                message = "WRITE NOT PERMITTED";
            } else {
                message = "UNKNOWN RESPONSE (" + status + ")";
            }
            Log.w(TAG, "onServicesDiscovered " + message);
        }

    };
}
