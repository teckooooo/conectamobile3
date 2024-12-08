package com.example.conectamobile;

public class Message {
    private String sender;
    private String senderName;
    private String receiver;
    private String message;
    private long timestamp;
    private String sender_receiver; // Campo adicional
    private String receiver_sender; // Campo adicional

    // Constructor vacío necesario para Firebase
    public Message() {}

    // Constructor con los nuevos campos
    public Message(String sender, String senderName, String receiver, String message, long timestamp) {
        this.sender = sender;
        this.senderName = senderName;
        this.receiver = receiver;
        this.message = message;
        this.timestamp = timestamp;

        // Inicializa los nuevos campos automáticamente
        this.sender_receiver = sender + "_" + receiver;
        this.receiver_sender = receiver + "_" + sender;
    }

    // Getters y Setters
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender_receiver() {
        return sender_receiver;
    }

    public void setSender_receiver(String sender_receiver) {
        this.sender_receiver = sender_receiver;
    }

    public String getReceiver_sender() {
        return receiver_sender;
    }

    public void setReceiver_sender(String receiver_sender) {
        this.receiver_sender = receiver_sender;
    }
}
