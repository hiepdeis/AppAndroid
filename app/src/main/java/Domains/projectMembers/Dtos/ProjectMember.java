package Domains.projectMembers.Dtos;

public class ProjectMember {
    private int userId;
    private String fullname;
    private String email;
    private String projectRole;
    private String status;

    public ProjectMember(int userId, String fullname, String email, String projectRole, String status) {
        this.userId = userId;
        this.fullname = fullname;
        this.email = email;
        this.projectRole = projectRole;
        this.status = status;
    }

    public int getUserId() { return userId; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }
    public String getProjectRole() { return projectRole; }
    public String getStatus() { return status; }
}
