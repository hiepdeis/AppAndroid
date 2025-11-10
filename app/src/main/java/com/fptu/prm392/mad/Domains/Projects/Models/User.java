package com.fptu.prm392.mad.Domains.Projects.Models;

public class User {
    private String userId;
    private String displayName;
    private String email;
    private String fullname;
    private String avatar;


    public User() {} // Bắt buộc có constructor trống cho Firestore

    // Getter & Setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
