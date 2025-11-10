package com.fptu.prm392.mad.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddUserAdapter extends RecyclerView.Adapter<AddUserAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> usersFiltered;
    private Set<String> selectedUserIds;
    private OnSelectionChangedListener selectionListener;
    private OnAddUserListener addUserListener;
    private boolean isMultiSelectMode = false;

    // Interface for single user add (old flow - for project members)
    public interface OnAddUserListener {
        void onAddUser(User user);
    }

    // Interface for multiple selection (new flow - for group chat)
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    // Constructor for single add mode (old flow)
    public AddUserAdapter(OnAddUserListener listener) {
        this.users = new ArrayList<>();
        this.usersFiltered = new ArrayList<>();
        this.addUserListener = listener;
        this.isMultiSelectMode = false;
    }

    // Constructor for multi-select mode (new flow)
    public AddUserAdapter(OnSelectionChangedListener listener) {
        this.users = new ArrayList<>();
        this.usersFiltered = new ArrayList<>();
        this.selectedUserIds = new HashSet<>();
        this.selectionListener = listener;
        this.isMultiSelectMode = true;
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

    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
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
        private ImageView ivUserAvatar;
        private TextView tvUserName;
        private TextView tvUserEmail;
        private CheckBox cbSelectUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            cbSelectUser = itemView.findViewById(R.id.cbSelectUser);
        }

        public void bind(User user) {
            // Set user info
            if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                tvUserName.setText(user.getFullname());
            } else {
                tvUserName.setText(user.getEmail());
            }

            tvUserEmail.setText(user.getEmail());

            if (isMultiSelectMode) {
                // Multi-select mode: Show checkbox, handle selection
                cbSelectUser.setVisibility(View.VISIBLE);
                cbSelectUser.setChecked(selectedUserIds != null && selectedUserIds.contains(user.getUserId()));

                // Handle item click
                itemView.setOnClickListener(v -> {
                    boolean isSelected = !cbSelectUser.isChecked();
                    cbSelectUser.setChecked(isSelected);

                    if (isSelected) {
                        selectedUserIds.add(user.getUserId());
                    } else {
                        selectedUserIds.remove(user.getUserId());
                    }

                    if (selectionListener != null) {
                        selectionListener.onSelectionChanged(selectedUserIds.size());
                    }
                });

                // Handle checkbox click
                cbSelectUser.setOnClickListener(v -> {
                    boolean isSelected = cbSelectUser.isChecked();

                    if (isSelected) {
                        selectedUserIds.add(user.getUserId());
                    } else {
                        selectedUserIds.remove(user.getUserId());
                    }

                    if (selectionListener != null) {
                        selectionListener.onSelectionChanged(selectedUserIds.size());
                    }
                });
            } else {
                // Single add mode: Hide checkbox, handle add on click
                cbSelectUser.setVisibility(View.GONE);

                // Handle item click to add user
                itemView.setOnClickListener(v -> {
                    if (addUserListener != null) {
                        addUserListener.onAddUser(user);
                    }
                });
            }
        }
    }
}

