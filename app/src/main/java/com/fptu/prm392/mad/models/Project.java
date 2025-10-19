package com.fptu.prm392.mad.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Project {
    private String projectId;
    private String name;
    private String description;
    private String createdBy;       // userId của người tạo
    @ServerTimestamp
    private Date createdAt;
    private int memberCount;        // Số lượng members
    private int taskCount;          // Số lượng tasks

    // Constructor mặc định
    public Project() {
    }

    // Constructor
    public Project(String projectId, String name, String description, String createdBy) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = new Date();
        this.memberCount = 1; // Creator là member đầu tiên
        this.taskCount = 0;
    }

    // Getters và Setters
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }
}
