package com.fptu.prm392.mad.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.utils.AvatarLoader;

import java.util.ArrayList;
import java.util.List;

public class UserChatAdapter extends RecyclerView.Adapter<UserChatAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> usersFiltered;
    private final OnChatClickListener chatClickListener;

    public interface OnChatClickListener {
        void onChatClick(User user);
    }

    public UserChatAdapter(OnChatClickListener listener) {
        this.users = new ArrayList<>();
        this.usersFiltered = new ArrayList<>();
        this.chatClickListener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        this.usersFiltered = new ArrayList<>(users);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        usersFiltered.clear();

        if (TextUtils.isEmpty(query)) {
            usersFiltered.addAll(users);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (User user : users) {
                String fullname = user.getFullname() != null ? user.getFullname().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

                if (fullname.contains(lowerQuery) || email.contains(lowerQuery)) {
                    usersFiltered.add(user);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_chat, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersFiltered.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return usersFiltered.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivUserAvatar;
        private final TextView tvUserName;
        private final TextView tvUserEmail;
        private final ImageView btnChat;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            btnChat = itemView.findViewById(R.id.btnChat);
        }

        public void bind(User user) {
            // Load avatar using AvatarLoader
            AvatarLoader.loadAvatar(itemView.getContext(), user.getAvatar(), ivUserAvatar);

            // Set user info
            if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                tvUserName.setText(user.getFullname());
            } else {
                tvUserName.setText(user.getEmail());
            }

            tvUserEmail.setText(user.getEmail());

            // Handle chat button click
            btnChat.setOnClickListener(v -> {
                if (chatClickListener != null) {
                    chatClickListener.onChatClick(user);
                }
            });
        }
    }
}

