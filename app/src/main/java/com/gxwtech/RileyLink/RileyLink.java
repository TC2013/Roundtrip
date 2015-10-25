package com.gxwtech.RileyLink;

import android.content.Context;

import com.gxwtech.rtdemo.bluetooth.BluetoothConnection;
import com.gxwtech.rtdemo.bluetooth.GattAttributes;
import com.gxwtech.rtdemo.bluetooth.GattCharacteristicReadCallback;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicReadOperation;
import com.gxwtech.rtdemo.bluetooth.operations.GattCharacteristicWriteOperation;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by geoff on 7/10/15.
 */
public class RileyLink {
    private ArrayList<UUID> mServiceUUIDs = new ArrayList<>();
    private String mAddress;
    Context mContext;
    public RileyLink(Context context, String address) {
        mContext = context;
        mAddress = address;
        mServiceUUIDs.add(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE));
        mServiceUUIDs.add(UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_SERVICE));
    }
    public void setServiceUUIDs(ArrayList<UUID> serviceUUIDs) {
        mServiceUUIDs = serviceUUIDs;
    }
    public ArrayList<UUID> getServiceUUIDs() {
        return mServiceUUIDs;
    }
    public String getAddress() {
        return mAddress;
    }
    // This affects the transmitter radio frequency
    // valid channels are zero through 4. (0-4)
    public boolean setTransmitChannel(int channelNumber) {
        BluetoothConnection.getInstance(mContext).queue(new GattCharacteristicWriteOperation(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_TX_CHANNEL_UUID), new byte[]{0x02}, false, false));
        return true;
    }

    // This affects the receiver radio frequency
    // valid channels are zero through 4. (0-4)
    public boolean setReceiveChannel(int channelNumber) {
        BluetoothConnection.getInstance(mContext).queue(new GattCharacteristicWriteOperation(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_RX_CHANNEL_UUID), new byte[]{0x02}, false, false));
        return true;
    }

    public boolean writeWithChecksum(final byte[] pkt) {
        write(RileyLinkUtil.appendChecksum(pkt));
        return true;
    }

    public boolean write(final byte[] pkt)
    {
        final byte[] minimedRFData = RileyLinkUtil.encodeData(pkt);
        BluetoothConnection.getInstance(mContext).queue(new GattCharacteristicWriteOperation(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_TX_PACKET_UUID), minimedRFData, false, false));
        // when writing to TxTrigger, the data doesn't matter -- only the act of writing.
        BluetoothConnection.getInstance(mContext).queue(new GattCharacteristicWriteOperation(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_TX_TRIGGER_UUID), new byte[]{0x01}, false, false));
        return true;
    }

    public boolean read(GattCharacteristicReadCallback cb) {
        BluetoothConnection.getInstance(mContext).queue(new GattCharacteristicReadOperation(UUID.fromString(GattAttributes.GLUCOSELINK_RILEYLINK_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_RX_PACKET_UUID), cb));
        return true;
    }

    public boolean readBatteryLevel(GattCharacteristicReadCallback cb) {
        BluetoothConnection.getInstance(mContext).queue(new GattCharacteristicReadOperation(UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_SERVICE),
                UUID.fromString(GattAttributes.GLUCOSELINK_BATTERY_UUID),cb));
        return true;
    }





}
