package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.fptu.prm392.mad.utils.AvatarLoader;

import java.util.ArrayList;
import java.util.List;

public class SelectedAssigneeAdapter extends RecyclerView.Adapter<SelectedAssigneeAdapter.ViewHolder> {

    private List<ProjectMember> selectedMembers;
    private final OnAssigneeRemoveListener removeListener;
    private final OnAssigneeClickListener clickListener;
    private boolean isCreator = false;
    private final UserRepository userRepository;

    public interface OnAssigneeRemoveListener {
        void onRemoveAssignee(ProjectMember member, int position);
    }

    public interface OnAssigneeClickListener {
        void onAssigneeClick(ProjectMember member);
    }

    public void setIsCreator(boolean isCreator) {
        this.isCreator = isCreator;
        notifyDataSetChanged();
    }

    public SelectedAssigneeAdapter(OnAssigneeRemoveListener removeListener) {
        this.selectedMembers = new ArrayList<>();
        this.removeListener = removeListener;
        this.clickListener = null;
        this.userRepository = new UserRepository();
    }

    public SelectedAssigneeAdapter(OnAssigneeRemoveListener removeListener, OnAssigneeClickListener clickListener) {
        this.selectedMembers = new ArrayList<>();
        this.removeListener = removeListener;
        this.clickListener = clickListener;
        this.userRepository = new UserRepository();
    }

    public void setSelectedMembers(List<ProjectMember> members) {
        this.selectedMembers = new ArrayList<>(members);
        notifyDataSetChanged();
    }

    public void addMember(ProjectMember member) {
        if (!containsMember(member)) {
            selectedMembers.add(member);
            notifyItemInserted(selectedMembers.size() - 1);
        }
    }

    public void removeMember(int position) {
        if (position >= 0 && position < selectedMembers.size()) {
            selectedMembers.remove(position);
            notifyItemRemoved(position);
        }
    }

    public boolean containsMember(ProjectMember member) {
        for (ProjectMember m : selectedMembers) {
            if (m.getUserId().equals(member.getUserId())) {
                return true;
            }
        }
        return false;
    }

    public List<ProjectMember> getSelectedMembers() {
        return new ArrayList<>(selectedMembers);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_assignee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectMember member = selectedMembers.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return selectedMembers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAssigneeAvatar;
        TextView tvAssigneeName;
        ImageView btnRemoveAssignee;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAssigneeAvatar = itemView.findViewById(R.id.ivAssigneeAvatar);
            tvAssigneeName = itemView.findViewById(R.id.tvAssigneeName);
            btnRemoveAssignee = itemView.findViewById(R.id.btnRemoveAssignee);
        }

        public void bind(ProjectMember member) {
            // Load avatar
            if (member.getAvatar() != null && !member.getAvatar().isEmpty()) {
                AvatarLoader.loadAvatar(itemView.getContext(), member.getAvatar(), ivAssigneeAvatar);
            } else {
                // Fallback: Load from User repository
                userRepository.getUserById(member.getUserId(),
                    user -> AvatarLoader.loadAvatar(itemView.getContext(), user.getAvatar(), ivAssigneeAvatar),
                    e -> ivAssigneeAvatar.setImageResource(R.drawable.profile)
                );
            }

            String displayName = member.getFullname() != null && !member.getFullname().isEmpty()
                    ? member.getFullname()
                    : member.getEmail();
            tvAssigneeName.setText(displayName);

            // Show/hide remove button based on listener and isCreator
            if (removeListener != null && isCreator) {
                btnRemoveAssignee.setVisibility(View.VISIBLE);
                btnRemoveAssignee.setOnClickListener(v ->
                    removeListener.onRemoveAssignee(member, getBindingAdapterPosition())
                );
            } else {
                btnRemoveAssignee.setVisibility(View.GONE);
            }

            // Handle item click to view profile
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAssigneeClick(member);
                }
            });
        }
    }
}

