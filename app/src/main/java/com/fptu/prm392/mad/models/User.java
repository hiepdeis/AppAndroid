package com.fptu.prm392.mad.models;

import com.google.firebase.Timestamp;

public class User {
    private String userId;          // Firebase Auth UID
    private String email;
    private String fullname;
    private String avatar;          // URL hoặc null
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructor mặc định (required cho Firestore)
    public User() {
    }

    // Constructor đầy đủ
    public User(String userId, String email, String fullname, String avatar) {
        this.userId = userId;
        this.email = email;
        this.fullname = fullname;
        this.avatar = avatar;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters và Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    // Helper method for display name
    public String getDisplayName() {
        return fullname != null ? fullname : email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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
