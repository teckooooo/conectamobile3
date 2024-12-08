package com.example.conectamobile;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttServicio {

    private MqttClient client;
    private MqttCallback callback;
    private String brokerUrl;
    private String clientId;
    private MqttConnectOptions connectOptions;

    public MqttServicio(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;

        try {
            // Inicializamos el cliente MQTT
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, clientId, persistence);
            connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
        } catch (MqttException e) {
            throw new RuntimeException("No se pudo crear el cliente MQTT", e);
        }
    }

    // Establecer el callback para manejar los eventos del MQTT
    public void setCallback(MqttCallback callback) {
        this.callback = callback;
        if (client != null) {
            client.setCallback(callback);
        }
    }

    // Conectar al broker MQTT
    public void connect() {
        try {
            if (!client.isConnected()) {
                client.connect(connectOptions);
                System.out.println("Conexión exitosa al broker MQTT");
            }
        } catch (MqttException e) {
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }
    }

    // Desconectar del broker MQTT
    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                System.out.println("Desconexión exitosa del broker MQTT");
            }
        } catch (MqttException e) {
            throw new RuntimeException("No se pudo desconectar del broker MQTT", e);
        }
    }

    // Publicar un mensaje en un tópico
    public void publish(String topic, String message) {
        if (isConnected()) {
            try {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(1); // Establecer el QoS (Quality of Service)
                client.publish(topic, mqttMessage);
                System.out.println("Mensaje publicado en el tópico: " + topic);
            } catch (MqttException e) {
                throw new RuntimeException("No se pudo publicar el mensaje", e);
            }
        } else {
            System.out.println("El cliente no está conectado, no se puede publicar.");
        }
    }

    // Suscribirse a un tópico
    public void subscribe(String topic) {
        if (isConnected()) {
            try {
                client.subscribe(topic);
                System.out.println("Suscrito al tópico: " + topic);
            } catch (MqttException e) {
                throw new RuntimeException("No se pudo suscribir al tópico", e);
            }
        } else {
            System.out.println("El cliente no está conectado, no se puede suscribir.");
        }
    }

    // Verificar si el cliente está conectado
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    // Intentar reconectar al broker si la conexión se pierde
    public void reconnect() {
        while (!isConnected()) {
            try {
                System.out.println("Intentando reconectar...");
                client.connect(connectOptions);
                System.out.println("Reconectado exitosamente al broker MQTT");
            } catch (MqttException e) {
                try {
                    Thread.sleep(5000);  // Esperar 5 segundos antes de intentar reconectar
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
