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
        loadMessagesFromDatabase();

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
                        String currentUserId = currentUser.getUid(); // ID del usuario autenticado
                        String senderName = currentUser.getDisplayName(); // Nombre del usuario actual

                        if (senderName == null) {
                            senderName = "Nombre no disponible"; // Si el nombre no está disponible
                        }

                        // Asumimos que el topic contiene el ID del receptor
                        String receiverId = topic;

                        // Consulta para obtener el nombre del destinatario desde la base de datos
                        String finalSenderName = senderName;
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(receiverId)
                                .get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        // Obtén el nombre del destinatario
                                        String receiverName = task.getResult().child("name").getValue(String.class);

                                        if (receiverName == null) {
                                            receiverName = "Destinatario no disponible"; // Si no existe el nombre
                                        }

                                        // Solo añadir el mensaje si todos los datos son válidos
                                        Message msg = new Message(
                                                currentUserId,
                                                finalSenderName,
                                                receiverName,
                                                receivedMessage,
                                                System.currentTimeMillis()
                                        );

                                        // Agregar el nuevo mensaje a la lista
                                        messageList.add(msg);

                                        // Mostrar los datos en la consola para depuración
                                        System.out.println("Sender ID: " + currentUserId);
                                        System.out.println("Sender Name: " + finalSenderName);
                                        System.out.println("Receiver ID: " + receiverId);
                                        System.out.println("Receiver Name: " + receiverName);
                                        System.out.println("Received Message: " + receivedMessage);
                                        System.out.println("Timestamp: " + System.currentTimeMillis());

                                        // Notificar al adaptador que la lista ha cambiado para actualizar la UI
                                        messagesAdapter.notifyDataSetChanged();
                                    } else {
                                        // Manejar errores si no se puede obtener el nombre del destinatario
                                        Toast.makeText(Chat.this, "No se pudo obtener los datos del destinatario", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Manejar el caso cuando el usuario no está autenticado
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

    public void loadMessagesFromDatabase() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages");

        // Obtener todos los mensajes de la base de datos
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Limpiar la lista de mensajes antes de agregar los nuevos
                messageList.clear();

                // Iterar sobre todos los mensajes en la base de datos
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Obtener los datos del mensaje
                    String senderId = snapshot.child("sender").getValue(String.class);
                    String receiverId = snapshot.child("receiver").getValue(String.class);
                    String senderName = snapshot.child("senderName").getValue(String.class);
                    String messageContent = snapshot.child("message").getValue(String.class);
                    long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    // Mostrar los datos obtenidos con println
                    System.out.println("Sender ID: " + senderId);
                    System.out.println("Receiver ID: " + receiverId);
                    System.out.println("Sender Name: " + senderName);
                    System.out.println("Message: " + messageContent);
                    System.out.println("Timestamp: " + timestamp);

                    // Crear un objeto Message con los datos obtenidos
                    Message msg = new Message(senderId, senderName, receiverId, messageContent, timestamp);

                    // Agregar el mensaje a la lista
                    messageList.add(msg);
                }

                // Notificar al adaptador que los datos han cambiado
                messagesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Manejo de errores si la consulta falla
                Toast.makeText(Chat.this, "Error al cargar los mensajes", Toast.LENGTH_SHORT).show();
            }
        });
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
