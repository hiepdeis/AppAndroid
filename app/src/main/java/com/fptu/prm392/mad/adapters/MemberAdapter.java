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

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<ProjectMember> members;
    private OnMemberActionListener listener;

    public interface OnMemberActionListener {
        void onDeleteMember(ProjectMember member, int position);
    }

    public MemberAdapter(OnMemberActionListener listener) {
        this.members = new ArrayList<>();
        this.listener = listener;
    }

    public void setMembers(List<ProjectMember> members) {
        // Sort members: Owner first, then others
        this.members = new ArrayList<>(members);
        this.members.sort((m1, m2) -> {
            // Owner always comes first
            if (m1.isOwner()) return -1;
            if (m2.isOwner()) return 1;
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
        holder.bind(member, position + 1); // Position + 1 for numbering from 1
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeleteMember;
        TextView tvNo, tvMemberName, tvMemberRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDeleteMember = itemView.findViewById(R.id.ivDeleteMember);
            tvNo = itemView.findViewById(R.id.tvNo);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
        }

        public void bind(ProjectMember member, int number) {
            // Set number
            tvNo.setText(String.valueOf(number));

            // Set name
            tvMemberName.setText(member.getFullname() != null ? member.getFullname() : member.getEmail());

            // Set role with appropriate styling
            String role = member.getProjectRole();
            tvMemberRole.setText(capitalizeFirst(role));

            if ("owner".equals(role)) {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_done); // Green
                // Hide delete icon for owner but keep the space
                ivDeleteMember.setVisibility(View.INVISIBLE);
            } else if ("admin".equals(role)) {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_in_progress); // Yellow
                // Show delete icon for admin
                ivDeleteMember.setVisibility(View.VISIBLE);
            } else {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_todo); // Gray
                // Show delete icon for member
                ivDeleteMember.setVisibility(View.VISIBLE);
            }

            // Delete click listener (only for non-owners)
            if (!member.isOwner()) {
                ivDeleteMember.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteMember(member, getAdapterPosition());
                    }
                });
            }
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }
}

