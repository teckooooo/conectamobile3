package com.example.conectamobile;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messageList;
    private String currentUserId;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;

        // Obtener el ID del usuario actual usando Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : null;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.senderNameTextView.setText(message.getSenderName());
        holder.messageTextView.setText(message.getMessage());

        // Verifica si el mensaje es del usuario actual o de otro
        if (message.getSender().equals(currentUserId)) {
            // Si el mensaje es del usuario actual, alineamos a la derecha
            holder.messageTextView.setGravity(Gravity.END);
            holder.messageTextView.setBackgroundResource(R.drawable.bg_message_sent); // Fondo diferente para mensajes del usuario
        } else {
            // Si el mensaje es de otra persona, alineamos a la izquierda
            holder.messageTextView.setGravity(Gravity.START);
            holder.messageTextView.setBackgroundResource(R.drawable.bg_message_received); // Fondo diferente para mensajes de otros usuarios
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView;
        TextView messageTextView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}
