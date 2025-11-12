package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;

public class Notification {
    private String notificationId;
    private String userId;          // User who receives the notification
    private String type;            // e.g., "task_assigned", "project_created", "member_added", "task_updated", "task_deleted"
    private String title;
    private String content;
    private String projectId;       // Optional: link to project
    private String taskId;          // Optional: link to task
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {
    }

    public Notification(String notificationId, String userId, String type, String title, String content, String projectId, String taskId) {
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

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}


