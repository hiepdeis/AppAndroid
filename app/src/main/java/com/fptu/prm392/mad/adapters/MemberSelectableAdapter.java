package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.ProjectMember;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemberSelectableAdapter extends RecyclerView.Adapter<MemberSelectableAdapter.ViewHolder> {

    private List<ProjectMember> allMembers;
    private List<ProjectMember> filteredMembers;
    private Set<String> selectedMemberIds;

    public MemberSelectableAdapter() {
        this.allMembers = new ArrayList<>();
        this.filteredMembers = new ArrayList<>();
        this.selectedMemberIds = new HashSet<>();
    }

    public void setMembers(List<ProjectMember> members) {
        this.allMembers = new ArrayList<>(members);
        this.filteredMembers = new ArrayList<>(members);
        notifyDataSetChanged();
    }

    public void setSelectedMembers(List<ProjectMember> selected) {
        selectedMemberIds.clear();
        for (ProjectMember member : selected) {
            selectedMemberIds.add(member.getUserId());
        }
        notifyDataSetChanged();
    }

    public List<ProjectMember> getSelectedMembers() {
        List<ProjectMember> selected = new ArrayList<>();
        for (ProjectMember member : allMembers) {
            if (selectedMemberIds.contains(member.getUserId())) {
                selected.add(member);
            }
        }
        return selected;
    }

    public void filter(String query) {
        filteredMembers.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredMembers.addAll(allMembers);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (ProjectMember member : allMembers) {
                String name = member.getFullname() != null ? member.getFullname().toLowerCase() : "";
                String email = member.getEmail() != null ? member.getEmail().toLowerCase() : "";
                if (name.contains(lowerQuery) || email.contains(lowerQuery)) {
                    filteredMembers.add(member);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_selectable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectMember member = filteredMembers.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return filteredMembers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvMemberName, tvMemberEmail, tvMemberRole;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberEmail = itemView.findViewById(R.id.tvMemberEmail);
            tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
        }

        public void bind(ProjectMember member) {
            String displayName = member.getFullname() != null && !member.getFullname().isEmpty()
                    ? member.getFullname()
                    : "No name";
            tvMemberName.setText(displayName);
            tvMemberEmail.setText(member.getEmail());

            // Set role
            String role = member.getProjectRole();
            tvMemberRole.setText(capitalizeFirst(role));
            if ("owner".equals(role)) {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_done);
            } else if ("admin".equals(role)) {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_in_progress);
            } else {
                tvMemberRole.setBackgroundResource(R.drawable.bg_status_todo);
            }

            // Set checkbox state
            boolean isSelected = selectedMemberIds.contains(member.getUserId());
            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(isSelected);

            // Handle checkbox click
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedMemberIds.add(member.getUserId());
                } else {
                    selectedMemberIds.remove(member.getUserId());
                }
            });

            // Handle item click
            itemView.setOnClickListener(v -> cbSelect.setChecked(!cbSelect.isChecked()));
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
    }
}

