package com.fptu.prm392.mad.models;

public class GlobalSearchResult {

    public enum ResultType {
        HEADER,
        PROJECT,
        TASK,
        USER
    }

    private ResultType type;
    private String headerTitle;
    private int resultCount;

    // Actual data objects
    private Project project;
    private Task task;
    private User user;

    // For projects: indicate if user is already a member
    private boolean isUserMember;

    // Constructor for HEADER
    public GlobalSearchResult(String headerTitle, int resultCount) {
        this.type = ResultType.HEADER;
        this.headerTitle = headerTitle;
        this.resultCount = resultCount;
    }

    // Constructor for PROJECT
    public GlobalSearchResult(Project project, boolean isUserMember) {
        this.type = ResultType.PROJECT;
        this.project = project;
        this.isUserMember = isUserMember;
    }

    // Constructor for TASK
    public GlobalSearchResult(Task task) {
        this.type = ResultType.TASK;
        this.task = task;
    }

    // Constructor for USER
    public GlobalSearchResult(User user) {
        this.type = ResultType.USER;
        this.user = user;
    }

    // Getters
    public ResultType getType() {
        return type;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public int getResultCount() {
        return resultCount;
    }

    public Project getProject() {
        return project;
    }

    public Task getTask() {
        return task;
    }

    public User getUser() {
        return user;
    }

    public boolean isUserMember() {
        return isUserMember;
    }
}

