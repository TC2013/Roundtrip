package com.gxwtech.rtdemo.uploader;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by Fokko on 7-8-15.
 */
public class MqttUploader {
    private final String topic = "/downloads/protobuf";
    private final int qos = 2;
    private final String broker = "tcp://broker.nightscout-docker2.fokko.svc.tutum.io:1883";
    private final String clientId = "Roundtrip";

    private static MqttUploader instance = null;

    public static MqttUploader getInstance() throws MqttException {
        if (instance == null) {
            synchronized (MqttUploader.class) {
                if (instance == null) {
                    instance = new MqttUploader();
                }
            }
        }
        return instance;
    }

    private static final String LS = System.getProperty("line.separator");
    private static final String TAG = "MqttUploader";

    private MqttClient sampleClient = null;

    protected MqttUploader() throws MqttException {
        sampleClient = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        sampleClient.connect(connOpts);
    }

    public void publishMessage(byte[] data) throws MqttException {
        MqttMessage message = new MqttMessage(data);
        message.setQos(qos);
        sampleClient.publish(topic, message);
    }

    public void disconnect() throws MqttException {
        sampleClient.disconnect();
        sampleClient = null;
    }
}
