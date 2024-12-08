package com.example.conectamobile;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Objects;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessagesAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageTextView.setText(message.getMessage());

        // Obtén el ID del usuario actual
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Compara el ID del remitente con el usuario actual
        if (message.getSender().equals(currentUserId)) {
            // Alinea el mensaje a la derecha
            System.out.println(currentUserId);
            System.out.println(message.getSender());
            holder.messageTextView.setGravity(Gravity.END);
            holder.messageTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            holder.messageTextView.setBackgroundResource(R.drawable.bg_message_sent); // Un fondo opcional para el mensaje enviado
        } else {
            // Alinea el mensaje a la izquierda
            System.out.println(currentUserId);
            System.out.println(message.getSender());
            holder.messageTextView.setGravity(Gravity.START);
            holder.messageTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            holder.messageTextView.setBackgroundResource(R.drawable.bg_message_received); // Un fondo opcional para el mensaje recibido
        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}





