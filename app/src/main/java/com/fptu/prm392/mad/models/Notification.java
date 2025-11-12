package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;

public class Notification {
    private String notificationId;
    private String userId;           // Người nhận notification
    private String type;             // "request_rejected", "invitation_rejected"
    private String title;
    private String message;
    private String projectId;
    private String projectName;
    private boolean isRead;
    private Timestamp createdAt;

    // Constructor mặc định
    public Notification() {
    }

    // Constructor đầy đủ
    public Notification(String notificationId, String userId, String type,
                       String title, String message, String projectId,
                       String projectName) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.projectId = projectId;
        this.projectName = projectName;
        this.isRead = false;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

