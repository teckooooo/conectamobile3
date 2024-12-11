package com.example.conectamobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eclipse.paho.client.mqttv3.*;

import java.util.ArrayList;
import java.util.List;

public class Chat extends AppCompatActivity {

    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "e47f841114984935a368a10f149dd356";
    private MqttServicio mqttService;
    private EditText messageEditText;
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;
    private Handler mainHandler;
    private DatabaseReference messagesRef;
    private static String topic = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeUI();
        initializeFirebase();
        setupMQTT();
        loadChatHistory();
    }

    private void initializeUI() {
        messageEditText = findViewById(R.id.messageInput);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        Button publishButton = findViewById(R.id.sendButton);
        TextView contactNameTextView = findViewById(R.id.contactNameTextView);

        messageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messageList);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messagesAdapter);

        publishButton.setOnClickListener(v -> sendMessage());

        String contactName = getIntent().getStringExtra("contactName");
        contactNameTextView.setText(contactName);

        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initializeFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = currentUser != null ? currentUser.getEmail() : "";
        String contactEmail = getIntent().getStringExtra("contactEmail");

        topic = generateTopic(currentUserEmail, contactEmail);
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
    }

    private void setupMQTT() {
        mqttService = new MqttServicio(BROKER_URL, CLIENT_ID);
        mqttService.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                showToast("Conexión perdida: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String receivedMessage = new String(message.getPayload());
                handleIncomingMessage(receivedMessage);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                showToast("Mensaje enviado correctamente.");
            }
        });
    }

    private void loadChatHistory() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String contactId = getIntent().getStringExtra("contactId");

        messagesRef.orderByChild("sender_receiver")
                .equalTo(currentUserId + "_" + contactId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }
                        messagesAdapter.notifyDataSetChanged();
                        messagesRecyclerView.smoothScrollToPosition(messageList.size() - 1); // Desplazar al final
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Error al cargar el historial: " + error.getMessage());
                    }
                });
    }


    private void sendMessage() {
        String messageContent = messageEditText.getText().toString().trim();
        if (messageContent.isEmpty()) {
            showToast("El mensaje no puede estar vacío.");
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String senderName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        String receiverId = getIntent().getStringExtra("contactId");

        Message message = new Message(senderId, senderName, receiverId, messageContent, System.currentTimeMillis());
        messagesRef.push().setValue(message)
                .addOnSuccessListener(aVoid -> {
                    mqttService.publish(topic, messageContent);
                    messageEditText.setText("");
                })
                .addOnFailureListener(e -> showToast("Error al enviar mensaje: " + e.getMessage()));
    }

    private void handleIncomingMessage(String receivedMessage) {
        // Manejar mensaje recibido aquí.
        showToast("Mensaje recibido: " + receivedMessage);
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(Chat.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mqttService.isConnected()) {
            mqttService.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mqttService.disconnect();
    }

    private String generateTopic(String currentUserEmail, String contactEmail) {
        System.out.println("chat/" + sanitizeEmail(currentUserEmail) + "_" + sanitizeEmail(contactEmail));
        return "chat/" + sanitizeEmail(currentUserEmail) + "_" + sanitizeEmail(contactEmail);
    }

    private String sanitizeEmail(String email) {
        return email.replace(".", "_");
    }
}
