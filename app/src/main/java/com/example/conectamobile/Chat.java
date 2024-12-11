package com.example.conectamobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
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
import java.util.Arrays;
import java.util.List;

public class Chat extends AppCompatActivity {

    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "e47f841114984935a368a10f149dd356";
    private MqttServicio mqttService;
    private EditText messageEditText;
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;
    private DatabaseReference messagesRef;
    private String topic = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeUI();
        initializeMessagingService();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = currentUser != null ? currentUser.getEmail() : "";
        String contactEmail = getIntent().getStringExtra("contactEmail");
        topic = generateTopic(currentUserEmail, contactEmail);

        String contactName = getIntent().getStringExtra("contactName");
        TextView contactNameTextView = findViewById(R.id.contactNameTextView);
        contactNameTextView.setText(contactName);

        subscribeToTopic(topic);
    }

    private void initializeUI() {
        messageList = new ArrayList<>();
        messageEditText = findViewById(R.id.messageInput);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        Button publishButton = findViewById(R.id.sendButton);

        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(messageList);
        messagesRecyclerView.setAdapter(messagesAdapter);

        publishButton.setOnClickListener(v -> publishMessage(topic, messageEditText.getText().toString()));

        loadMessagesFromDatabase();
    }

    private void initializeMessagingService() {
        mqttService = new MqttServicio(BROKER_URL, FirebaseAuth.getInstance().getCurrentUser().getUid());
        mqttService.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                runOnUiThread(() -> Toast.makeText(Chat.this, "Conexión perdida: " + cause.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                processIncomingMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    private void processIncomingMessage(String topic, MqttMessage message) {
        String receivedMessage = new String(message.getPayload());
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            String senderName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Nombre no disponible";
            String receiverId = topic;

            FirebaseDatabase.getInstance().getReference("users")
                    .child(receiverId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String receiverName = task.getResult().child("name").getValue(String.class);
                            if (receiverName == null) {
                                receiverName = "Destinatario no disponible";
                            }

                            Message msg = new Message(currentUserId, senderName, receiverName, receivedMessage, System.currentTimeMillis());
                            if (isMessageForCurrentChat(currentUserId, receiverId)) {
                                messageList.add(msg);
                                messagesAdapter.notifyDataSetChanged();
                                messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    });
        } else {
            Toast.makeText(Chat.this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
        loadChatHistory(getIntent().getStringExtra("contactId"));
    }

    private void loadMessagesFromDatabase() {
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message msg = snapshot.getValue(Message.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }
                messagesAdapter.notifyDataSetChanged();
                messagesRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private boolean isMessageForCurrentChat(String senderId, String receiverId) {
        String contactId = getIntent().getStringExtra("contactId");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return (senderId.equals(currentUserId) && receiverId.equals(contactId)) ||
                (receiverId.equals(currentUserId) && senderId.equals(contactId));
    }

    private void loadChatHistory(String contactId) {
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Eliminar notificación de error
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mqttService.connect();
        subscribeToTopic(topic);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mqttService.disconnect();
    }

    private void publishMessage(String topic, String message) {
        if (mqttService.isConnected()) {
            mqttService.publish(topic, message);

            String sender = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String senderName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            String receiver = getIntent().getStringExtra("contactId");
            long timestamp = System.currentTimeMillis();

            Message messageObject = new Message(sender, senderName, receiver, message, timestamp);
            messagesRef.push().setValue(messageObject)
                    .addOnSuccessListener(aVoid -> {
                        messageEditText.setText("");
                    })
                    .addOnFailureListener(e -> {
                    });
        } else {
            Toast.makeText(this, "No estás conectado a MQTT", Toast.LENGTH_SHORT).show();
        }
    }

    private void subscribeToTopic(String topic) {
        mqttService.subscribe(topic);
    }

    private String generateTopic(String currentUserEmail, String contactEmail) {
        String currentUserName = currentUserEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String contactUserName = contactEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String[] sortedNames = {currentUserName, contactUserName};
        Arrays.sort(sortedNames);
        System.out.println("chat/" + sortedNames[0] + "_" + sortedNames[1]);
        return "chat/" + sortedNames[0] + "_" + sortedNames[1];
    }
}

