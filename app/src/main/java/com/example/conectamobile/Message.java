package com.example.conectamobile;

public class Message {
    private String sender;
    private String senderName;
    private String receiver;
    private String message;
    private long timestamp;

    public Message(String sender, String senderName, String receiver, String message, long timestamp) {
        this.sender = sender;
        this.senderName = senderName;
        this.receiver = receiver;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
