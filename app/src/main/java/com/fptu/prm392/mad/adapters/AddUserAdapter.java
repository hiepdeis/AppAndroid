package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.User;

import java.util.ArrayList;
import java.util.List;

public class AddUserAdapter extends RecyclerView.Adapter<AddUserAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> usersFiltered;
    private OnAddUserListener listener;

    public interface OnAddUserListener {
        void onAddUser(User user);
    }

    public AddUserAdapter(OnAddUserListener listener) {
        this.users = new ArrayList<>();
        this.usersFiltered = new ArrayList<>();
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        this.usersFiltered = new ArrayList<>(users);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        usersFiltered.clear();
        if (query == null || query.trim().isEmpty()) {
            usersFiltered.addAll(users);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (User user : users) {
                String name = user.getFullname() != null ? user.getFullname().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

                if (name.contains(lowerQuery) || email.contains(lowerQuery)) {
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
                .inflate(R.layout.item_user_add, parent, false);
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
        TextView tvUserName, tvUserEmail;
        ImageView btnAddUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            btnAddUser = itemView.findViewById(R.id.btnAddUser);
        }

        public void bind(User user) {
            // Set name
            tvUserName.setText(user.getFullname() != null ? user.getFullname() : "Unknown");

            // Set email
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            // Add button click
            btnAddUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddUser(user);
                }
            });
        }
    }
}

