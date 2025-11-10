package com.fptu.prm392.mad.Domains.Projects.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.fptu.prm392.mad.Domains.Projects.Dtos.Member;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class Project implements Parcelable {

    private String projectId;
    private String name;
    private String description;
    private String createdBy;
    private String createdByName; // có thể null
    private Timestamp createdAt;  // kiểu Timestamp của Firebase
    private int memberCount;
    private int taskCount;
    private List<Member> memberIds;

    // 🔹 Constructor mặc định (Firebase cần)
    public Project() {
        this.projectId = "";
        this.name = "";
        this.description = "";
        this.createdBy = "";
        this.createdByName = null;
        this.createdAt = null;
        this.memberCount = 0;
        this.taskCount = 0;
        this.memberIds = new ArrayList<>();
    }

    // 🔹 Constructor đầy đủ
    public Project(String projectId, String name, String description, String createdBy,
                   String createdByName, Timestamp createdAt, int memberCount,
                   int taskCount, List<Member> memberIds) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
        this.taskCount = taskCount;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
    }

    // 🔹 Getter & Setter
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }

    public List<Member> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Member> memberIds) { this.memberIds = memberIds; }

    // 🔹 Parcelable Implementation
    protected Project(Parcel in) {
        projectId = in.readString();
        name = in.readString();
        description = in.readString();
        createdBy = in.readString();
        createdByName = in.readString();
        createdAt = in.readParcelable(Timestamp.class.getClassLoader());
        memberCount = in.readInt();
        taskCount = in.readInt();
        memberIds  = in.createTypedArrayList(Member.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(projectId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(createdBy);
        dest.writeString(createdByName);
        dest.writeParcelable(createdAt, flags);
        dest.writeInt(memberCount);
        dest.writeInt(taskCount);
        dest.writeTypedList(memberIds );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Project> CREATOR = new Creator<Project>() {
        @Override
        public Project createFromParcel(Parcel in) {
            return new Project(in);
        }

        @Override
        public Project[] newArray(int size) {
            return new Project[size];
        }
    };
}
