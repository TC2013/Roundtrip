package com.gxwtech.rtdemo.bluetooth;


import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Geoff on 7/10/15.
 */
public class GattAttributes {
    // iOS app allows abbreviations of UUIDs, but Android does not:
    //public static String GLUCOSELINK_BATTERY_SERVICE = "180f";
    // NOTE: these uuid strings must be lower case!
    // The battery is handled in a separate Service
    public static final String GLUCOSELINK_BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static final String GLUCOSELINK_BATTERY_UUID = "00002a19-0000-1000-8000-00805f9b34fb";
    // This is the main RileyLink Service
    public static final String GLUCOSELINK_RILEYLINK_SERVICE = "d39f1890-17eb-11e4-8c21-0800200c9a66";
    // read this to read the next packet in the receive queue
    // returns empty if no packets.
    public static final String GLUCOSELINK_RX_PACKET_UUID = "2fb1a490-1940-11e4-8c21-0800200c9a66";
    // receive channel, zero to 4 (0-4) (use 2)
    public static final String GLUCOSELINK_RX_CHANNEL_UUID = "d93b2af0-1ea8-11e4-8c21-0800200c9a66";
    // trasmit channel, zero to 4 (0-4) (use 2)
    public static final String GLUCOSELINK_TX_CHANNEL_UUID = "d93b2af0-1458-11e4-8c21-0800200c9a66";
    // packet counter -- increases when a packet is received, decreases when a packet is read
    // you can set Notify on (the first property of) this characteristic
    public static final String GLUCOSELINK_PACKET_COUNT = "41825a20-7402-11e4-8c21-0800200c9a66";
    // write a packet to this address (in RF code format!)
    public static final String GLUCOSELINK_TX_PACKET_UUID = "2fb1a490-1941-11e4-8c21-0800200c9a66";
    // write any value to TX_trigger to send the packet in tx_packet
    public static final String GLUCOSELINK_TX_TRIGGER_UUID = "2fb1a490-1942-11e4-8c21-0800200c9a66";
    // you can write to custom_name to store your own identifier for this device.
    public static final String GLUCOSELINK_CUSTOM_NAME = "d93b2af0-1e28-11e4-8c21-0800200c9a66";
    private static HashMap<String, String> attributes = new HashMap<String, String>();

    static {
        // Sample Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

        attributes.put(GLUCOSELINK_BATTERY_SERVICE, "RileyLink Battery Service");
        attributes.put(GLUCOSELINK_BATTERY_UUID, "RileyLink Battery Level");

        attributes.put(GLUCOSELINK_RILEYLINK_SERVICE, "RileyLink Service");
        attributes.put(GLUCOSELINK_RX_CHANNEL_UUID, "RileyLink RX Channel");
        attributes.put(GLUCOSELINK_TX_CHANNEL_UUID, "RileyLink TX Channel");
        attributes.put(GLUCOSELINK_PACKET_COUNT, "RileyLink Packet Count");
        attributes.put(GLUCOSELINK_RX_PACKET_UUID, "RileyLink RX Packet");
        attributes.put(GLUCOSELINK_TX_PACKET_UUID, "RileyLink TX Packet");
        attributes.put(GLUCOSELINK_TX_TRIGGER_UUID, "RileyLink TX Trigger");
    }

    public static String lookup(UUID uuid) {
        return lookup(uuid.toString());
    }

    public static String lookup(String uuid) {
        return lookup(uuid, uuid);
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
