package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;

/**
 * Model cho Notification - lưu vào Firestore để hiển thị trong-app
 */
public class Notification {
    private String notificationId;
    private String userId;              // User nhận notification
    private String type;                // "task_assigned", "task_updated", "member_added", "project_updated", etc.
    private String title;
    private String content;
    private String projectId;           // Nullable
    private String taskId;              // Nullable
    private boolean isRead;
    private Timestamp createdAt;

    // Constructor mặc định (required cho Firestore)
    public Notification() {
        this.isRead = false;
        this.createdAt = Timestamp.now();
    }

    // Constructor đầy đủ
    public Notification(String notificationId, String userId, String type, 
                       String title, String content, String projectId, String taskId) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.projectId = projectId;
        this.taskId = taskId;
        this.isRead = false;
        this.createdAt = Timestamp.now();
    }

    // Getters và Setters
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    // Helper methods
    public boolean isTaskNotification() {
        return type != null && (type.equals("task_assigned") || type.equals("task_updated"));
    }

    public boolean isMemberNotification() {
        return type != null && (type.equals("member_added") || type.equals("member_removed"));
    }

    public boolean isProjectNotification() {
        return type != null && type.equals("project_updated");
    }
}

