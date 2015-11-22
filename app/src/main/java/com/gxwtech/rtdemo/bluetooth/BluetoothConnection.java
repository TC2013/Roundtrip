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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gxwtech.rtdemo.Constants;
import com.gxwtech.rtdemo.HexDump;
import com.gxwtech.rtdemo.Intents;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicReadOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattDescriptorReadOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattDiscoverServices;
import com.gxwtech.rtdemo.bluetooth.operations.GattInitializeBluetooth;
import com.gxwtech.rtdemo.bluetooth.operations.GattOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattSetNotificationOperation;
import com.gxwtech.rtdemo.decoding.packages.MedtronicPackage;
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

    private static BluetoothConnection instance = null;
    private final Context context;

    private GattOperation mCurrentOperation = null;

    private AsyncTask<Void, Void, Void> mCurrentOperationTimeout;
    private BluetoothGatt bluetoothConnectionGatt = null;
    private ConcurrentLinkedQueue<GattOperation> mQueue = new ConcurrentLinkedQueue<>();


    final BluetoothManager bluetoothManager;
    final BluetoothAdapter bluetoothAdapter;


    protected BluetoothConnection(Context context) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
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

    public BluetoothDevice getDevice() {
        if (bluetoothConnectionGatt!=null) {
            return bluetoothConnectionGatt.getDevice();
        }
        return null;
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

        mQueue.clear();
        setCurrentOperation(null);

        queue(new GattInitializeBluetooth());
    }

    public final synchronized void queue(GattOperation gattOperation) {
        mQueue.add(gattOperation);
        if(mCurrentOperation == null) {
            drive();
        }else {
            Log.v(TAG, "Queueing Gatt operation " + gattOperation.toString() + ", size: " + mQueue.size());
        }
    }

    public synchronized void setCurrentOperation(GattOperation currentOperation) {
        mCurrentOperation = currentOperation;
        if (currentOperation != null) {
            Log.v(TAG, "Current operation: " + mCurrentOperation.toString());
        } else {
            Log.v(TAG, "Current Operation has been finished");

            if (mCurrentOperationTimeout != null) {
                mCurrentOperationTimeout.cancel(true);
                mCurrentOperationTimeout = null;
            }

            drive();
        }
    }

    public synchronized void drive() {
        if (mCurrentOperation != null) {
            Log.v(TAG, "Still a query running (" + mCurrentOperation + "), waiting...");
            return;
        }

        if (mQueue.size() == 0) {
            Log.v(TAG, "Queue empty, drive loop stopped.");
            return;
        }

        setCurrentOperation(mQueue.poll());

        mCurrentOperationTimeout = new AsyncTask<Void, Void, Void>() {
            @Override
            protected synchronized Void doInBackground(Void... voids) {
                try {
                    Log.v(TAG, "Setting timeout for: " + mCurrentOperation.toString());
                    wait(22 * 1000);

                    if (isCancelled()) {
                        Log.v(TAG, "The timeout has already been cancelled.");
                    } else if (null == mCurrentOperation) {
                        Log.v(TAG, "The timeout was cancelled and the query was successful, so we do nothing.");
                    } else {
                        Log.v(TAG, "Timeout ran to completion, time to cancel the operation. Abort ships!");

                        setCurrentOperation(null);
                        return null;
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

        if (this.bluetoothAdapter != null && this.bluetoothAdapter.isEnabled()) {

            if (bluetoothConnectionGatt != null) {
                execute(bluetoothConnectionGatt, mCurrentOperation);
            } else {
                final ScanSettings settings = new ScanSettings.Builder().build();

                // This comes in handy when using a BLE smartwatch :)
                final ArrayList<ScanFilter> filters = new ArrayList<>();
                final ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(Constants.PrefName.Bluetooth_RileyLink_Address).build();
                filters.add(filter);

                final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

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

                        disconnect();
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

                                    Log.w(TAG, "onCharacteristicChanged " + GattAttributes.lookup(characteristic.getUuid()) + " " + HexDump.toHexString(characteristic.getValue()));
                                    if(characteristic.getUuid().equals(UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT))) {
                                        // If there are packages waiting, please download them.
                                        if(characteristic.getValue()[0] > 0) {
                                            queue(new GattCharacteristicReadOperation(
                                                    UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                                                    UUID.fromString(GattAttributes.GLUCOSELINK_RX_PACKET_UUID),
                                                    null
                                            ));
                                        }
                                    }
                                }

                                @Override
                                public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
                                    super.onCharacteristicRead(gatt, characteristic, status);

                                    final String statusMessage = getGattStatusMessage(status);

                                    if (characteristic.getUuid().toString().equals(GattAttributes.GLUCOSELINK_RX_PACKET_UUID)) {
                                        byte[] data = characteristic.getValue();

                                        final MedtronicPackage pack = Decoder.DeterminePackage(data);

                                        if (pack != null) {
                                            Log.w(TAG, "Got valid package: " + pack.toString() + " raw data: " + HexDump.toHexString(data));
                                        } else {
                                            Log.w(TAG, "Could not determine package from bytes " + HexDump.toHexString(data));
                                        }

                                    } else if (characteristic.getUuid().toString().equals(GattAttributes.GLUCOSELINK_PACKET_COUNT)) {

                                        Log.w(TAG, "Found number of packets: " + HexDump.toHexString(characteristic.getValue()));

                                        if (characteristic.getValue()[0] > 0) {
                                            queue(new GattCharacteristicReadOperation(
                                                    UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                                                    UUID.fromString(GattAttributes.GLUCOSELINK_RX_PACKET_UUID),
                                                    null
                                            ));
                                        }
                                    } else {
                                        Log.w(TAG, "onCharacteristicRead (" + GattAttributes.lookup(characteristic.getUuid()) + ") "
                                                + statusMessage + ":" + HexDump.toHexString(characteristic.getValue()));
                                    }

                                    ((GattCharacteristicReadOperation) mCurrentOperation).onRead(characteristic);

                                    setCurrentOperation(null);
                                    drive();
                                }

                                @Override
                                public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
                                    super.onCharacteristicWrite(gatt, characteristic, status);

                                    final String uuidString = GattAttributes.lookup(characteristic.getUuid());
                                    Log.w(TAG, "onCharacteristicWrite " + getGattStatusMessage(status) + " " + uuidString + " " + HexDump.toHexString(characteristic.getValue()));

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
                                        bluetoothConnectionGatt = gatt;
                                        //batteryHandler.postDelayed(batteryTask, BATTERY_UPDATE);
                                        queue(new GattDiscoverServices());

                                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.BLUETOOTH_CONNECTED));
                                    } else {
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.BLUETOOTH_DISCONNECTED));
                                        disconnect();

                                        Log.w(TAG, "Cannot establish Bluetooth connection.");
                                    }

                                    setCurrentOperation(null);
                                }


                                @Override
                                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                    super.onDescriptorWrite(gatt, descriptor, status);

                                    Log.w(TAG, "onDescriptorWrite "
                                            + GattAttributes.lookup(descriptor.getUuid()) + " "
                                            + getGattStatusMessage(status)
                                            + " written: " + HexDump.toHexString(descriptor.getValue()));

                                    setCurrentOperation(null);
                                }

                                @Override
                                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                    super.onDescriptorRead(gatt, descriptor, status);

                                    ((GattDescriptorReadOperation) mCurrentOperation).onRead(descriptor);

                                    Log.w(TAG, "onDescriptorRead " + getGattStatusMessage(status) + " status " + descriptor);

                                    setCurrentOperation(null);
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

                                    if (status == BluetoothGatt.GATT_SUCCESS) {
                                        final List<BluetoothGattService> services = gatt.getServices();
                                        for (BluetoothGattService service : services) {
                                            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                                            final UUID uuidService = service.getUuid();
                                            final String uuidServiceString = uuidService.toString();

                                            String debugString = "Found service: " + GattAttributes.lookup(uuidServiceString, "Unknown device") + " (" + uuidServiceString + ")" + LS;

                                            for (BluetoothGattCharacteristic character : characteristics) {
                                                if (service.getUuid().equals(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE)) &&
                                                        character.getUuid().equals(UUID.fromString(GattAttributes.GLUCOSELINK_PACKET_COUNT))) {

                                                    for(BluetoothGattDescriptor descriptor : character.getDescriptors()) {
                                                        queue(new GattSetNotificationOperation(
                                                                service.getUuid(),
                                                                character.getUuid(),
                                                                descriptor.getUuid()
                                                        ));
                                                    }
                                                }

                                                final String uuidCharacteristicString = character.getUuid().toString();
                                                debugString += "    - " + GattAttributes.lookup(uuidCharacteristicString) + LS;
                                            }
                                            Log.w(TAG, debugString);
                                        }

                                    }

                                    Log.w(TAG, "onServicesDiscovered " + getGattStatusMessage(status));

                                    setCurrentOperation(null);
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

    private synchronized void execute(BluetoothGatt gatt, GattOperation operation) {
        if (operation != mCurrentOperation) {
            Log.e(TAG, "Already other service running!!");
            return;
        }

        operation.execute(gatt);

        if (!operation.hasAvailableCompletionCallback()) {
            setCurrentOperation(null);
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
