package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String content;
    private Timestamp timestamp;
    private boolean isRead;

    // Constructor mặc định (required cho Firestore)
    public Message() {
    }

    // Constructor đầy đủ
    public Message(String messageId, String chatId, String senderId, String senderName, String content) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = Timestamp.now();
        this.isRead = false;
    }

    // Getters và Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}

