package com.gxwtech.RileyLink;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by geoff on 7/10/15.
 */
public class RileyLink {
    private ArrayList<UUID> mServiceUUIDs = new ArrayList<>();
    private String mAddress;
    public RileyLink(String address) {
        mAddress = address;
        mServiceUUIDs.add(UUID.fromString(GattAttributes.GLUCOSELINK_SERVICE_UUID));
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

}
