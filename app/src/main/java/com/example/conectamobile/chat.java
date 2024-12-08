package com.example.conectamobile;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.*;

public class chat extends AppCompatActivity {

    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "e47f841114984935a368a10f149dd356";
    private MqttServicio mqttService;  // Cambiar a MqttServicio
    private EditText messageEditText;
    private TextView messagesTextView;
    private Handler mainHandler;

    private static final String TOPIC = "chat/messages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mqttService = new MqttServicio(BROKER_URL, CLIENT_ID);  // Usar MqttServicio

        messageEditText = findViewById(R.id.messageInput);
        messagesTextView = findViewById(R.id.chatDisplay);
        Button publishButton = findViewById(R.id.sendButton);

        mainHandler = new Handler(Looper.getMainLooper());

        mqttService.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                mainHandler.post(() ->
                        Toast.makeText(chat.this, "Conexión perdida: " + cause.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                mainHandler.post(() -> {
                    String receivedMessage =  new String(message.getPayload());
                    messagesTextView.append(receivedMessage + "\n");
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                mainHandler.post(() ->
                        Toast.makeText(chat.this, "Entrega completa", Toast.LENGTH_SHORT).show()
                );
            }
        });

        publishButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            publicandoMensaje(TOPIC, message);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mqttService.connect();
        subscribiendoTopico(TOPIC);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mqttService.disconnect();
    }

    private void publicandoMensaje(String topic, String message) {
        if (mqttService.isConnected()) {
            mqttService.publish(topic, message);
        } else {
            Toast.makeText(this, "El cliente no está conectado al broker.", Toast.LENGTH_SHORT).show();
        }
    }

    private void subscribiendoTopico(String topic) {
        if (mqttService.isConnected()) {
            mqttService.subscribe(topic);
        } else {
            Toast.makeText(this, "El cliente no está conectado al broker.", Toast.LENGTH_SHORT).show();
        }
    }
}
