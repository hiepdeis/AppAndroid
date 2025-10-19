package com.fptu.prm392.mad.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Project {
    private String projectId;
    private String name;
    private String description;
    private String createdBy;       // userId của người tạo
    private String createdByName;   // Tên người tạo (denormalized)
    @ServerTimestamp
    private Date createdAt;
    private int memberCount;        // Số lượng members
    private int taskCount;          // Số lượng tasks
    private List<String> memberIds; // Danh sách userId của members - để query dễ dàng

    // Constructor mặc định
    public Project() {
        this.memberIds = new ArrayList<>();
    }

    // Constructor
    public Project(String projectId, String name, String description, String createdBy, String createdByName) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = new Date();
        this.memberCount = 1; // Creator là member đầu tiên
        this.taskCount = 0;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(createdBy); // Thêm creator vào memberIds
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

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
}
