package com.gxwtech.rtdemo.bluetooth;

import android.bluetooth.BluetoothAdapter;
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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicReadOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattDescriptorReadOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattSetNotificationOperation;
import com.gxwtech.rtdemo.decoding.DataPackage;
import com.gxwtech.rtdemo.decoding.Decoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Fokko on 2-8-15.
 */
public class BluetoothConnection {
    private static final String LS = System.getProperty("line.separator");
    private static final String TAG = "BluetoothConnection";
    private final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    // http://www.mopri.de/2010/timertask-bad-do-it-the-android-way-use-a-handler/
    private static final int BATTERY_UPDATE = 60 * 1000; // Every minute
    private static BluetoothConnection instance = null;
    private final Context context;
    private GattOperation mCurrentOperation;
    private AsyncTask<Void, Void, Void> mCurrentOperationTimeout;
    private BluetoothGatt bluetoothConnectionGatt = null;
    private ConcurrentLinkedQueue<GattOperation> mQueue = new ConcurrentLinkedQueue<>();
    private Handler batteryHandler = new Handler();
    private Runnable batteryTask = new Runnable() {
        @Override
        public void run() {
            /* do what you need to do */
            instance.queue(new GattCharacteristicReadOperation(
                    UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_SERVICE),
                    UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_UUID),
                    new GattCharacteristicReadCallback() {
                        @Override
                        public void call(byte[] characteristic) {
                            Intent batteryUpdate = new Intent(Intents.BLUETOOTH_BATTERY);
                            batteryUpdate.putExtra("battery", characteristic[0]);

                            LocalBroadcastManager.getInstance(context).sendBroadcast(batteryUpdate);
                        }
                    }
            ));

            /* and here comes the "trick" */
            batteryHandler.postDelayed(this, BATTERY_UPDATE);
        }
    };

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

    public static String toHexString(byte[] array) {
        return toHexString(array, 0, array.length);
    }

    public static String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];
        for (int i = offset; i < offset + length; i++) {
            int b = array[i] & 0xFF;
            buf[i * 2] = HEX_DIGITS[b >>> 4];
            buf[i * 2 + 1] = HEX_DIGITS[b & 0x0F];
        }

        return new String(buf);
    }

    public void disconnect() {
        Log.w(TAG, "Closing GATT connection");

        // Close old conenction
        if (bluetoothConnectionGatt != null) {
            // Not sure if to disconnect or to close first..
            bluetoothConnectionGatt.disconnect();
            bluetoothConnectionGatt.close();
            bluetoothConnectionGatt = null;
        }

        setCurrentOperation(null);
    }

    public synchronized void queue(GattOperation gattOperation) {
        if (gattOperation != null) {
            mQueue.add(gattOperation);
            Log.v(TAG, "Queueing Gatt operation " + gattOperation.toString() + ", size: " + mQueue.size());
            drive();
        }
    }

    public synchronized void cancelCurrentOperation() {
        Log.v(TAG, "Cancelling current operation. Queue size: " + mQueue.size());

        setCurrentOperation(null);
        drive();
    }

    public synchronized void setCurrentOperation(GattOperation currentOperation) {
        if (currentOperation != null) {
            Log.v(TAG, "Current operation: " + currentOperation.toString());
        } else {
            Log.v(TAG, "Current operation: null");
        }
        mCurrentOperation = currentOperation;
    }

    public synchronized void drive() {
        if (mCurrentOperation != null) {
            Log.v(TAG, "Still a query running, waiting...");
            return;
        }

        if (mCurrentOperationTimeout != null) {
            mCurrentOperationTimeout.cancel(true);
            mCurrentOperationTimeout = null;
        }

        if (mQueue.size() == 0) {
            Log.v(TAG, "Queue empty, drive loop stopped.");
            return;
        }

        final GattOperation operation = mQueue.poll();
        setCurrentOperation(operation);

        mCurrentOperationTimeout = new AsyncTask<Void, Void, Void>() {
            @Override
            protected synchronized Void doInBackground(Void... voids) {
                try {
                    Log.v(TAG, "Setting timeout for: " + operation.toString());
                    wait(22 * 1000);

                    if (isCancelled()) {
                        Log.v(TAG, "The timeout has already been cancelled.");
                    } else if (null == mCurrentOperation) {
                        Log.v(TAG, "The timeout was cancelled and the query was successful, so we do nothing.");
                    } else {
                        Log.v(TAG, "Timeout ran to completion, time to cancel the operation. Abort ships!");
                        cancelCurrentOperation();
                    }
                } catch (InterruptedException e) {
                    Log.v(TAG, "Timeout was stopped because of early success");
                }
                return null;
            }

            @Override
            protected synchronized void onCancelled() {
                super.onCancelled();
                notify();
            }
        }.execute();

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            //TODO: Look into Scanmode settings
            final ScanSettings settings = new ScanSettings.Builder().build();

            // This comes in handy when using a BLE smartwatch :)
            final ArrayList<ScanFilter> filters = new ArrayList<>();
            final ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(Constants.PrefName.Bluetooth_RileyLink_Address).build();
            filters.add(filter);

            final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

            if (bluetoothConnectionGatt != null) {
                execute(bluetoothConnectionGatt, operation);
            } else {
                Log.w(TAG, "Starting scan.");
                // Start new connection
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
                    public void onScanResult(final int callbackType, final ScanResult result) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.BLUETOOTH_CONNECTING));

                        Log.w(TAG, "Found device: " + result.getDevice().getAddress());

                        if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                            Log.w(TAG, "Found a suitable device, stopping the scan.");
                            scanner.stopScan(this);

                            // TODO: https://github.com/suzp1984/Light_BLE/blob/master/Light_BLE/ble/src/main/java/org/zpcat/ble/BluetoothLeService.java#L285
                            // Connect using Gatt, any further communication will be done using asynchronous calls.
                            result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {

                                @Override
                                public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
                                    super.onCharacteristicChanged(gatt, characteristic);

                                    Log.w(TAG, "onCharacteristicChanged " + GattAttributes.lookup(characteristic.getUuid()) + " " + toHexString(characteristic.getValue()));
                                }

                                @Override
                                public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
                                    super.onCharacteristicRead(gatt, characteristic, status);

                                    final String statusMessage = getGattStatusMessage(status);

                                    if (characteristic.getUuid().toString().equals(GattAttributes.GLUCOSELINK_RX_PACKET_UUID)) {
                                        byte[] data = characteristic.getValue();

                                        final DataPackage pack = Decoder.DeterminePackage(data);

                                        if (pack != null) {
                                            Log.w(TAG, "Got valid package: " + pack.toString() + " raw data: " + toHexString(data));
                                        } else {
                                            Log.w(TAG, "Could not determine package from bytes " + toHexString(data));
                                        }

                                    } else if (characteristic.getUuid().toString().equals(GattAttributes.GLUCOSELINK_PACKET_COUNT)) {

                                        Log.w(TAG, "Found number of packets: " + toHexString(characteristic.getValue()));

                                        /*
                                        if (characteristic.getValue()[0] > 0) {
                                            queue(new GattCharacteristicReadOperation(
                                                    UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                                                    UUID.fromString(GattAttributes.GLUCOSELINK_RX_PACKET_UUID),
                                                    null
                                            ));
                                        }*/
                                    } else {
                                        Log.w(TAG, "onCharacteristicRead (" + GattAttributes.lookup(characteristic.getUuid()) + ") "
                                                + statusMessage + ":" + toHexString(characteristic.getValue()));
                                    }

                                    ((GattCharacteristicReadOperation) mCurrentOperation).onRead(characteristic);

                                    setCurrentOperation(null);
                                    drive();
                                }

                                @Override
                                public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
                                    super.onCharacteristicWrite(gatt, characteristic, status);

                                    final String uuidString = GattAttributes.lookup(characteristic.getUuid());
                                    Log.w(TAG, "onCharacteristicWrite " + getGattStatusMessage(status) + " " + uuidString + " " + toHexString(characteristic.getValue()));

                                    setCurrentOperation(null);
                                    drive();
                                }

                                @Override
                                public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
                                    super.onConnectionStateChange(gatt, status, newState);

                                    // https://github.com/NordicSemiconductor/puck-central-android/blob/master/PuckCentral/app/src/main/java/no/nordicsemi/puckcentral/bluetooth/gatt/GattManager.java#L117
                                    if (status == 133) {
                                        Log.e(TAG, "Got the status 133 bug, closing gatt");
                                        disconnect();
                                        return;
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

                                    Log.w(TAG, "onConnectionStateChange " + getGattStatusMessage(status) + " " + stateMessage);

                                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                                        if (gatt.discoverServices()) {
                                            Log.w(TAG, "Starting to discover GATT Services.");

                                            bluetoothConnectionGatt = gatt;
                                            batteryHandler.postDelayed(batteryTask, BATTERY_UPDATE);
                                        } else {
                                            Log.w(TAG, "Cannot discover GATT Services.");
                                        }

                                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.BLUETOOTH_CONNECTED));
                                    } else {
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.BLUETOOTH_DISCONNECTED));
                                        disconnect();

                                        Log.w(TAG, "Cannot establish Bluetooth connection.");
                                    }
                                }


                                @Override
                                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                    super.onDescriptorWrite(gatt, descriptor, status);

                                    Log.w(TAG, "onDescriptorWrite "
                                            + GattAttributes.lookup(descriptor.getUuid()) + " "
                                            + getGattStatusMessage(status)
                                            + " written: " + toHexString(descriptor.getValue()));

                                    setCurrentOperation(null);
                                    drive();
                                }

                                @Override
                                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                    super.onDescriptorRead(gatt, descriptor, status);

                                    ((GattDescriptorReadOperation) mCurrentOperation).onRead(descriptor);

                                    Log.w(TAG, "onDescriptorRead " + getGattStatusMessage(status) + " status " + descriptor);

                                    setCurrentOperation(null);
                                    drive();
                                }

                                @Override
                                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                                    super.onMtuChanged(gatt, mtu, status);

                                    Log.w(TAG, "onMtuChanged " + mtu + " status " + status);
                                }

                                @Override
                                public void onReadRemoteRssi(final BluetoothGatt gatt, int rssi, int status) {
                                    super.onReadRemoteRssi(gatt, rssi, status);

                                    Log.w(TAG, "onReadRemoteRssi " + getGattStatusMessage(status) + ": " + rssi);
                                }

                                @Override
                                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                                    super.onReliableWriteCompleted(gatt, status);

                                    Log.w(TAG, "onReliableWriteCompleted status " + status);
                                }

                                @Override
                                public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                                    super.onServicesDiscovered(gatt, status);

                                    final String message;
                                    if (status == BluetoothGatt.GATT_SUCCESS) {
                                        final List<BluetoothGattService> services = gatt.getServices();
                                        for (BluetoothGattService service : services) {
                                            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                                            final UUID uuidService = service.getUuid();
                                            final String uuidServiceString = uuidService.toString();

                                            String debugString = "Found service: " + GattAttributes.lookup(uuidServiceString, "Unknown device") + " (" + uuidServiceString + ")" + LS;

                                            for (BluetoothGattCharacteristic character : characteristics) {
                                                if (character.getUuid().equals(UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT))) {
                                                    queue(new GattSetNotificationOperation(
                                                            UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                                                            UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT),
                                                            character.getDescriptors().get(0).getUuid()
                                                    ));
                                                }

                                                final String uuidCharacteristicString = character.getUuid().toString();
                                                debugString += "    - " + GattAttributes.lookup(uuidCharacteristicString) + LS;
                                            }
                                            Log.w(TAG, debugString);
                                        }

                                        message = "Got response, found " + services.size() + " devices so far.";

                                        execute(gatt, operation);
                                    } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                                        message = "WRITE NOT PERMITTED";
                                    } else {
                                        message = "UNKNOWN RESPONSE (" + status + ")";
                                    }

                                    Log.w(TAG, "onServicesDiscovered " + message);
                                }
                            });

                            Log.w(TAG, "RileyLink has been found, establishing connection.");
                        }
                    }
                });
            }
        } else {
            Log.e(TAG, "Could not find suitable bluetooth adapter.");
        }
    }

    private void execute(BluetoothGatt gatt, GattOperation operation) {
        if (operation != mCurrentOperation) {
            Log.e(TAG, "Already other service running!!");
            return;
        }

        operation.execute(gatt);

        if (!operation.hasAvailableCompletionCallback()) {
            setCurrentOperation(null);
            drive();
        }
    }

    private String getGattStatusMessage(final int status) {
        final String statusMessage;
        if (status == BluetoothGatt.GATT_SUCCESS) {
            statusMessage = "SUCCESS";
        } else if (status == BluetoothGatt.GATT_FAILURE) {
            statusMessage = "FAILED";
        } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
            statusMessage = "NOT PERMITTED";
        } else if (status == 133) {
            statusMessage = "Found the strange 133 bug";
        } else {
            statusMessage = "UNKNOWN (" + status + ")";
        }

        return statusMessage;
    }
}
