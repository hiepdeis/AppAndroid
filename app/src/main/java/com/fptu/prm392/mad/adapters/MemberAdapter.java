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

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<ProjectMember> members;
    private final OnMemberActionListener listener;
    private boolean isManager = false;
    private final UserRepository userRepository;

    public interface OnMemberActionListener {
        void onDeleteMember(ProjectMember member, int position);
    }

    public MemberAdapter(OnMemberActionListener listener) {
        this.members = new ArrayList<>();
        this.listener = listener;
        this.userRepository = new UserRepository();
    }

    public void setIsManager(boolean isManager) {
        this.isManager = isManager;
        notifyDataSetChanged();
    }

    public void setMembers(List<ProjectMember> members) {
        // Sort members: Manager first, then others
        this.members = new ArrayList<>(members);
        this.members.sort((m1, m2) -> {
            // Manager always comes first
            if (m1.isManager()) return -1;
            if (m2.isManager()) return 1;
            // Then sort by joinedAt (earliest first)
            if (m1.getJoinedAt() != null && m2.getJoinedAt() != null) {
                return m1.getJoinedAt().compareTo(m2.getJoinedAt());
            }
            return 0;
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        ProjectMember member = members.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMemberAvatar, ivDeleteMember;
        TextView tvMemberName, tvMemberRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            ivDeleteMember = itemView.findViewById(R.id.ivDeleteMember);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
        }

        public void bind(ProjectMember member) {
            // Load avatar from User collection
            if (member.getAvatar() != null && !member.getAvatar().isEmpty()) {
                AvatarLoader.loadAvatar(itemView.getContext(), member.getAvatar(), ivMemberAvatar);
            } else {
                // Load from User repository if avatar not in ProjectMember
                userRepository.getUserById(member.getUserId(),
                    user -> AvatarLoader.loadAvatar(itemView.getContext(), user.getAvatar(), ivMemberAvatar),
                    e -> ivMemberAvatar.setImageResource(R.drawable.profile)
                );
            }

            // Set name
            tvMemberName.setText(member.getFullname() != null ? member.getFullname() : member.getEmail());

            // Set role with appropriate styling
            String role = member.getProjectRole();
            tvMemberRole.setText(capitalizeFirst(role));

            if ("manager".equals(role)) {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_done); // Green
                // Always hide delete icon for manager
                ivDeleteMember.setVisibility(View.INVISIBLE);
            } else if ("admin".equals(role)) {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_in_progress); // Yellow
                // Show delete icon only if current user is manager
                ivDeleteMember.setVisibility(isManager ? View.VISIBLE : View.INVISIBLE);
            } else {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_todo); // Gray
                // Show delete icon only if current user is manager
                ivDeleteMember.setVisibility(isManager ? View.VISIBLE : View.INVISIBLE);
            }

            // Delete click listener (only for non-managers and if current user is manager)
            if (!member.isManager() && isManager) {
                ivDeleteMember.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteMember(member, getBindingAdapterPosition());
                    }
                });
            } else {
                ivDeleteMember.setOnClickListener(null);
            }
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }
}

