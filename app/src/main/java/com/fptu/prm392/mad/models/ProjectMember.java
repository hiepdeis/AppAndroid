package com.fptu.prm392.mad.models;

public class ProjectMember {
    private String projectId;       // ID của project mà member này thuộc về
    private String userId;
    private String fullname;        // Denormalized từ User
    private String email;           // Denormalized từ User
    private String avatar;          // Denormalized từ User
    private String projectRole;     // "manager", "admin", "member"
    private com.google.firebase.Timestamp joinedAt;

    // Constructor mặc định
    public ProjectMember() {
    }

    // Constructor
    public ProjectMember(String projectId, String userId, String fullname, String email, String avatar, String projectRole) {
        this.projectId = projectId;
        this.userId = userId;
        this.fullname = fullname;
        this.email = email;
        this.avatar = avatar;
        this.projectRole = projectRole;
        this.joinedAt = com.google.firebase.Timestamp.now();
    }

    // Getters và Setters
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getProjectRole() {
        return projectRole;
    }

    public void setProjectRole(String projectRole) {
        this.projectRole = projectRole;
    }

    public com.google.firebase.Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(com.google.firebase.Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    // Helper methods
    public boolean isManager() {
        return "manager".equals(projectRole);
    }

    public boolean isAdmin() {
        return "admin".equals(projectRole);
    }

    public boolean canManageProject() {
        return isManager() || isAdmin();
    }
}

