package com.example.conectamobile;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.*;

import org.eclipse.paho.client.mqttv3.*;

public class MqttServicio {
    private MqttClient client;
    private String brokerUrl;
    private String clientId;

    public MqttServicio(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;

        try {
            client = new MqttClient(brokerUrl, clientId, null);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            if (client != null && !client.isConnected()) {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                client.connect(options);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void publish(String topic, String message) {
        try {
            if (client != null && client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(1);
                client.publish(topic, mqttMessage);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic, 1);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void setCallback(MqttCallback callback) {
        if (client != null) {
            client.setCallback(callback);
        }
    }
}