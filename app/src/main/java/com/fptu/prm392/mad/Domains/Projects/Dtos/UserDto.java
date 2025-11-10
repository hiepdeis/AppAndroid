package com.fptu.prm392.mad.Domains.Projects.Dtos;

import android.os.Parcel;
import android.os.Parcelable;
import com.fptu.prm392.mad.Domains.Projects.Models.User;
public class UserDto extends User implements Parcelable  {
    private String role; // "manager" | "member" ...

    public UserDto() {
        super();
    }

    public UserDto(String userId, String displayName, String email, String fullname, String avatar, String role) {
        super();
        setUserId(userId);
        setDisplayName(displayName);
        setEmail(email);
        setFullname(fullname);
        setAvatar(avatar);
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // 🔹 Parcelable (để truyền qua Intent)
    protected UserDto(Parcel in) {
        setUserId(in.readString());
        setDisplayName(in.readString());
        setEmail(in.readString());
        setFullname(in.readString());
        setAvatar(in.readString());
        role = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getUserId());
        dest.writeString(getDisplayName());
        dest.writeString(getEmail());
        dest.writeString(getFullname());
        dest.writeString(getAvatar());
        dest.writeString(role);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserDto> CREATOR = new Creator<UserDto>() {
        @Override
        public UserDto createFromParcel(Parcel in) {
            return new UserDto(in);
        }

        @Override
        public UserDto[] newArray(int size) {
            return new UserDto[size];
        }
    };
}
