package com.example.conectamobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import org.eclipse.paho.client.mqttv3.*;

public class Chat extends AppCompatActivity {

    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "e47f841114984935a368a10f149dd356";
    private MqttServicio mqttService;  // Cambiar a MqttService
    private EditText messageEditText;
    private TextView messagesTextView;
    private Handler mainHandler;
    private DatabaseReference messagesRef;

    private static final String TOPIC = "chat/messages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mqttService = new MqttServicio(BROKER_URL, userUID);

        messageEditText = findViewById(R.id.messageInput);
        messagesTextView = findViewById(R.id.chatDisplay);
        Button publishButton = findViewById(R.id.sendButton);

        // Recupera los datos del Intent
        String contactId = getIntent().getStringExtra("contactId");
        String contactName = getIntent().getStringExtra("contactName");

        mainHandler = new Handler(Looper.getMainLooper());
        TextView contactNameTextView = findViewById(R.id.contactNameTextView);
        contactNameTextView.setText(contactName);

        // Establece el callback de MQTT
        mqttService.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                mainHandler.post(() ->
                        Toast.makeText(Chat.this, "Conexión perdida: " + cause.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                mainHandler.post(() -> {
                    String receivedMessage = new String(message.getPayload());
                    messagesTextView.append(receivedMessage + "\n");
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                mainHandler.post(() ->
                        Toast.makeText(Chat.this, "Entrega completa", Toast.LENGTH_SHORT).show()
                );
            }
        });

        publishButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            publicandoMensaje(TOPIC, message);
        });

        // Cargar el historial de mensajes desde Firebase
        loadChatHistory(contactId);
    }

    private void loadChatHistory(String contactId) {
        messagesRef = FirebaseDatabase.getInstance().getReference("message");

        // Consulta los mensajes del chat específico
        messagesRef.orderByChild("sender").equalTo(contactId).addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder chatHistory = new StringBuilder();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        // Mostrar el nombre del remitente junto con el mensaje
                        chatHistory.append(message.getSenderName()).append(": ").append(message.getMessage()).append("\n");
                    }
                }
                messagesTextView.setText(chatHistory.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Chat.this, "Error al cargar el historial: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

            // Obtener información del remitente y destinatario
            String sender = getIntent().getStringExtra("contactId"); // ID del usuario actual
            String senderName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName(); // Nombre del usuario actual
            String receiver = getIntent().getStringExtra("contactName"); // El destinatario
            long timestamp = System.currentTimeMillis();

            // Crear un objeto Message y guardarlo en Firebase
            Message messageObject = new Message(sender, senderName, receiver, message, timestamp);

            messagesRef = FirebaseDatabase.getInstance().getReference("messages");
            messagesRef.push().setValue(messageObject)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Mensaje guardado en Firebase.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al guardar mensaje: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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
