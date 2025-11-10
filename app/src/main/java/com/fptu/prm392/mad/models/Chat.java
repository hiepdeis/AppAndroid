package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    private String chatId;
    private String projectId;
    private String projectName;
    private List<String> participantIds;  // List of user IDs in this chat
    private String lastMessage;
    private String lastMessageSenderId;
    private Timestamp lastMessageTime;
    private Timestamp createdAt;
    private int unreadCount;  // Số tin nhắn chưa đọc

    // Constructor mặc định (required cho Firestore)
    public Chat() {
        this.participantIds = new ArrayList<>();
    }

    // Constructor đầy đủ
    public Chat(String chatId, String projectId, String projectName, List<String> participantIds) {
        this.chatId = chatId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.participantIds = participantIds != null ? participantIds : new ArrayList<>();
        this.createdAt = Timestamp.now();
        this.unreadCount = 0;
    }

    // Getters và Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}

