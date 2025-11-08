package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Task {
    private String taskId;
    private String projectId;
    private String title;
    private String description;
    private String status;          // "todo", "in_progress", "done"
    private List<String> assignees; // Array of userIds
    private String createdBy;       // userId của người tạo task
    private Timestamp createdAt;
    private Timestamp dueDate;      // Nullable

    // Constructor mặc định
    public Task() {
        this.assignees = new ArrayList<>();
    }

    // Constructor
    public Task(String taskId, String projectId, String title, String description, String createdBy) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = "todo"; // Mặc định là todo
        this.assignees = new ArrayList<>();
        this.createdBy = createdBy;
        this.createdAt = Timestamp.now();
    }

    // Getters và Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public List<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    // Helper methods
    public void addAssignee(String userId) {
        if (!assignees.contains(userId)) {
            assignees.add(userId);
        }
    }

    public void removeAssignee(String userId) {
        assignees.remove(userId);
    }

    public boolean isAssignedTo(String userId) {
        return assignees.contains(userId);
    }

    public boolean isTodo() {
        return "todo".equals(status);
    }

    public boolean isInProgress() {
        return "in_progress".equals(status);
    }

    public boolean isDone() {
        return "done".equals(status);
    }

    public boolean isCreator(String userId) {
        return createdBy != null && createdBy.equals(userId);
    }
}

