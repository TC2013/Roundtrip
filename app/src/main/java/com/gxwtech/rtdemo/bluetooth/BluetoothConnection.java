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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import com.gxwtech.rtdemo.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Fokko on 2-8-15.
 */
public class BluetoothConnection {
    private static final String LS = System.getProperty("line.separator");
    private static final String TAG = "BluetoothConnection";

    private static BluetoothGatt bluetoothConnectionGatt = null;

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
        // Close old conenction
        if(bluetoothConnectionGatt != null) {
            bluetoothConnectionGatt.disconnect();
            bluetoothConnectionGatt.close();
            bluetoothConnectionGatt = null;
        }

        String message;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothManager != null) {
            if (bluetoothAdapter.isEnabled()) {
                //TODO: Look into Scanmode settings
                ScanSettings settings = new ScanSettings.Builder().build();

                // This comes in handy when using a BLE smartwatch :)
                ArrayList<ScanFilter> filters = new ArrayList<>();
                ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(Constants.PrefName.Bluetooth_RileyLink_Address).build();
                filters.add(filter);

                final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

                scanner.startScan(filters, settings, new ScanCallback() {
                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        Log.w(TAG, "Batch results: " + results.size());
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.w(TAG, "Scan failed: " + errorCode);
                    }

                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.w(TAG, "Found device: " + result.getDevice().getAddress());

                        if(callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                            Log.w(TAG, "Found a suitable device, stopping the scan.");
                            scanner.stopScan(this);

                            // TODO: https://github.com/suzp1984/Light_BLE/blob/master/Light_BLE/ble/src/main/java/org/zpcat/ble/BluetoothLeService.java#L285
                            // Connect using Gatt, any further communication will be done using asynchronous calls.
                            bluetoothConnectionGatt = result.getDevice().connectGatt(context, true, mGattcallback);

                            Log.w(TAG, "RileyLink has been found, staring to establish connection.");
                        }
                    }
                });

                message = "Started scanning.";
            } else {
                message = "Bluetooth is not enabled on device.";
            }
        } else {
            message = "Bluetooth is not available on device.";
        }

        return message;
    }

    public void sendCommand(byte[] data, final String uuidService, final String uuidCharacteristic, final boolean transform, final boolean addCRC) {

        Log.d(TAG, "Sending package, pre-transform: " + BluetoothConnection.toHexString(data));
        if (transform) {
            data = RileyLinkUtil.composeRFStream(data);
            Log.d(TAG, "Sending, post-transform: " + BluetoothConnection.toHexString(data));
        }

        if (addCRC) {
            data = CRC.appendCRC(data);
        }

        if (bluetoothConnectionGatt == null) {
            Log.e(TAG, "GATT connection not available!");
            return;
        }

        final BluetoothGattService service = bluetoothConnectionGatt.
                getService(UUID.fromString(uuidService));

        if (service == null) {
            Log.e(TAG, "Service not found!");
            return;
        }

        final BluetoothGattCharacteristic characteristic = service.
                getCharacteristic(UUID.fromString(uuidCharacteristic));

        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found!");
            return;
        }

        characteristic.setValue(data);
        bluetoothConnectionGatt.writeCharacteristic(characteristic);

        Log.d(TAG, "Characteristic is being send.");
    }

    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            Log.w(TAG, "onCharacteristicChanged " + characteristic);


        }


        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, "onCharacteristicRead " + characteristic + " status " + status);


        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {

            final String statusMessage;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                statusMessage = "SUCCESS";
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                statusMessage = "FAILED";
            } else {
                statusMessage = "UNKNOWN (" + status + ")";
            }

            final String uuidString = GattAttributes.lookup(characteristic.getUuid());
            Log.w(TAG, "onCharacteristicWrite " + statusMessage + " " + uuidString + " " + toHexString(characteristic.getValue()));
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
                stateMessage = "UNKNOWN (" + newState + ")";
            }

            Log.w(TAG, "onConnectionStateChange " + statusMessage + " " + stateMessage);

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
            Log.w(TAG, "onDescriptorRead " + descriptor + " status " + status);


        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            final String statusMessage;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                statusMessage = "SUCCESS";
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                statusMessage = "FAILED";
            } else {
                statusMessage = "UNKNOWN (" + status + ")";
            }

            Log.w(TAG, "onDescriptorWrite " + GattAttributes.lookup(descriptor.getUuid()) + " " + statusMessage + " written: " + toHexString(descriptor.getValue()));
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.w(TAG, "onMtuChanged " + mtu + " status " + status);


        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.w(TAG, "onReadRemoteRssi " + rssi + " status " + status);


        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.w(TAG, "onReliableWriteCompleted status " + status);


        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            final String message;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    final UUID uuidService = service.getUuid();
                    final String uuidServiceString = uuidService.toString();

                    String debugString = "Found service: " + GattAttributes.lookup(uuidServiceString, "Unknown device") + " (" + uuidServiceString + ")" + LS;
                    for (BluetoothGattCharacteristic character : characteristics) {
                        final String descriptorType;

                        if (character.getUuid().equals(UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT))) {
                            for (BluetoothGattDescriptor descriptor : character.getDescriptors()) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                            descriptorType = "Notification";
                        } else if (character.getUuid().equals(UUID.fromString(GattAttributes.GLUCOSELINK_TX_PACKET_UUID))) {
                            for (BluetoothGattDescriptor descriptor : character.getDescriptors()) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                            descriptorType = "Indication";
                        } else {
                            descriptorType = "None";
                        }

                        final String uuidCharacteristicString = character.getUuid().toString();
                        debugString += "    - " + GattAttributes.lookup(uuidCharacteristicString) + " " + descriptorType + LS;
                    }
                    Log.w(TAG, debugString);
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


    public static String toHexString(byte[] array) {
        return toHexString(array, 0, array.length);
    }

    private final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];
        for (int i = offset; i < offset + length; i++) {
            int b = array[i] & 0xFF;
            buf[i * 2] = HEX_DIGITS[b >>> 4];
            buf[i * 2 + 1] = HEX_DIGITS[b & 0x0F];
        }

        return new String(buf);
    }
}
