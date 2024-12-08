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

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import org.eclipse.paho.client.mqttv3.*;

import java.util.ArrayList;
import java.util.List;

public class Chat extends AppCompatActivity {

    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "e47f841114984935a368a10f149dd356";
    private MqttServicio mqttService;
    private EditText messageEditText;
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter; // Usa el MessagesAdapter
    private List<Message> messageList;
    private Handler mainHandler;
    private DatabaseReference messagesRef;

    private static final String TOPIC = "chat/messages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageList = new ArrayList<>();
        mqttService = new MqttServicio(BROKER_URL, FirebaseAuth.getInstance().getCurrentUser().getUid());

        messageEditText = findViewById(R.id.messageInput);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        Button publishButton = findViewById(R.id.sendButton);

        // Recupera los datos del Intent
        String contactId = getIntent().getStringExtra("contactId");
        String contactName = getIntent().getStringExtra("contactName");

        mainHandler = new Handler(Looper.getMainLooper());
        TextView contactNameTextView = findViewById(R.id.contactNameTextView);
        contactNameTextView.setText(contactName);

        messagesAdapter = new MessagesAdapter(messageList); // Usamos el MessagesAdapter
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messagesAdapter); // Asignamos el adapter

        // Establece el callback de MQTT
        mqttService.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                mainHandler.post(() -> {
                    Toast.makeText(Chat.this, "Conexión perdida: " + cause.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                mainHandler.post(() -> {
                    // Obtén el mensaje recibido del payload
                    String receivedMessage = new String(message.getPayload());

                    // Obtén el ID del usuario actual (remitente)
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                    // Verifica si el usuario está autenticado
                    if (currentUser != null) {
                        String currentUserId = currentUser.getUid();  // ID del usuario autenticado
                        String senderName = currentUser.getDisplayName();  // Nombre del usuario actual

                        if (senderName == null) {
                            senderName = "Nombre no disponible";  // Si el nombre no está disponible
                        }

                        // Asumimos que 'receiver' es el ID del destinatario en la base de datos
                        String receiverId = "david";  // Este es el ID del destinatario, obtenido del payload

                        // Consulta para obtener el nombre del destinatario desde la base de datos
                        String finalSenderName = senderName;
                        String finalSenderName1 = senderName;
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(receiverId)
                                .get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Obtén el nombre del destinatario
                                        String receiverName = task.getResult().child("name").getValue(String.class);

                                        if (receiverName == null) {
                                            receiverName = "Destinatario no disponible"; // Si no existe el nombre
                                        }

                                        // Crear un objeto Message con los datos correctos
                                        Message msg = new Message(currentUserId, finalSenderName, receiverName, receivedMessage, System.currentTimeMillis());

                                        // Agregar el nuevo mensaje a la lista
                                        messageList.add(msg);
                                        System.out.println(finalSenderName1);
                                        System.out.println(receiverName);
                                        // Notificar al adaptador que la lista ha cambiado para actualizar la UI
                                        messagesAdapter.notifyDataSetChanged();
                                    } else {
                                        Toast.makeText(Chat.this, "Error al obtener el nombre del destinatario", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Si el usuario no está autenticado, puedes manejar este caso (mostrar mensaje de error, etc.)
                        Toast.makeText(Chat.this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
                    }
                });
            }






            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                mainHandler.post(() -> {
                    Toast.makeText(Chat.this, "Entrega completa", Toast.LENGTH_SHORT).show();
                });
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
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");

        // Consulta los mensajes del chat específico entre el usuario actual y el contacto
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef.orderByChild("sender_receiver")
                .equalTo(currentUserId + "_" + contactId)  // Asumiendo que creaste una combinación sender_receiver
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear(); // Limpia la lista antes de agregar los nuevos mensajes

                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                messageList.add(message);  // Agregar el nuevo mensaje a la lista
                            }
                        }

                        // Notifica al adaptador que la lista ha cambiado para actualizar la vista
                        messagesAdapter.notifyDataSetChanged();
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
            String sender = FirebaseAuth.getInstance().getCurrentUser().getUid(); // ID del usuario actual
            String senderName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName(); // Nombre del usuario actual
            String receiver = getIntent().getStringExtra("contactName"); // El destinatario
            long timestamp = System.currentTimeMillis();

            // Crear un objeto Message y guardarlo en Firebase
            Message messageObject = new Message(sender, senderName, receiver, message, timestamp);

            messagesRef.push().setValue(messageObject)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Mensaje guardado en Firebase.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al guardar mensaje: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No estás conectado a MQTT", Toast.LENGTH_SHORT).show();
        }
    }

    private void subscribiendoTopico(String topic) {
        mqttService.subscribe(topic);
    }
}
