package com.fptu.prm392.mad.Domains.Projects.Dtos;

import android.os.Parcel;
import android.os.Parcelable;
public class Member implements Parcelable{

    private String userId;
    private String role;

    // Default constructor (Firebase)
    public Member() {
        this.userId = "";
        this.role = "";
    }

    public Member(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Parcelable
    protected Member(Parcel in) {
        userId = in.readString();
        role = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(role);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel in) {
            return new Member(in);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };
}
