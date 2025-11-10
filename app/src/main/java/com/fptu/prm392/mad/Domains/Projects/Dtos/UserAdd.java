package com.fptu.prm392.mad.Domains.Projects.Dtos;

public class UserAdd {

    private String id;
    private String fullname;
    private String email;

    public UserAdd() {}

    public UserAdd(String id, String fullname, String email) {
        this.id = id;
        this.fullname = fullname;
        this.email = email;
    }

    public String getId() { return id; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }

    public void setId(String id) { this.id = id; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public void setEmail(String email) { this.email = email; }

}
