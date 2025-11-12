package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;

public class ProjectJoinRequest {
    private String requestId;
    private String projectId;
    private String projectName;
    private String requesterId;      // User muốn join hoặc được mời
    private String requesterName;
    private String requesterEmail;
    private String requesterAvatar;
    private String managerId;        // Manager của project hoặc người nhận request
    private String requestType;      // "join_request" (user xin vào) hoặc "invitation" (manager mời)
    private String status;           // "pending", "approved", "rejected"
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructor mặc định
    public ProjectJoinRequest() {
    }

    // Constructor đầy đủ
    public ProjectJoinRequest(String requestId, String projectId, String projectName,
                             String requesterId, String requesterName, String requesterEmail,
                             String requesterAvatar, String managerId, String requestType) {
        this.requestId = requestId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requesterAvatar = requesterAvatar;
        this.managerId = managerId;
        this.requestType = requestType;
        this.status = "pending";
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequesterAvatar() {
        return requesterAvatar;
    }

    public void setRequesterAvatar(String requesterAvatar) {
        this.requesterAvatar = requesterAvatar;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

