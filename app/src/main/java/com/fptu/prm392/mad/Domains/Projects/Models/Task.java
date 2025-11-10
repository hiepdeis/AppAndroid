package com.fptu.prm392.mad.Domains.Projects.Models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Task {
    private String taskId;
    private String title;
    private String description;
    private String createdBy;
    private Timestamp createdAt;
    private Timestamp dueDate;
    private boolean done;
    private boolean inProgress;
    private String projectId;
    private String status; // varchar(50) like: "todo", "inprogress", "done"
    private List<String> assignees;

    public Task() {}

    // getters / setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public boolean isInProgress() { return inProgress; }
    public void setInProgress(boolean inProgress) { this.inProgress = inProgress; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getAssignees() { return assignees; }
    public void setAssignees(List<String> assignees) { this.assignees = assignees; }

    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", dueDate=" + dueDate +
                ", done=" + done +
                ", inProgress=" + inProgress +
                ", projectId='" + projectId + '\'' +
                ", status='" + status + '\'' +
                ", assignees=" + assignees +
                '}';
    }

}
