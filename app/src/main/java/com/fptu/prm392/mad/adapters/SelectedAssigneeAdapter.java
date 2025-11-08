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

public class SelectedAssigneeAdapter extends RecyclerView.Adapter<SelectedAssigneeAdapter.ViewHolder> {

    private List<ProjectMember> selectedMembers;
    private OnAssigneeRemoveListener listener;

    public interface OnAssigneeRemoveListener {
        void onRemoveAssignee(ProjectMember member, int position);
    }

    public SelectedAssigneeAdapter(OnAssigneeRemoveListener listener) {
        this.selectedMembers = new ArrayList<>();
        this.listener = listener;
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
        TextView tvAssigneeName;
        ImageView btnRemoveAssignee;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAssigneeName = itemView.findViewById(R.id.tvAssigneeName);
            btnRemoveAssignee = itemView.findViewById(R.id.btnRemoveAssignee);
        }

        public void bind(ProjectMember member) {
            String displayName = member.getFullname() != null && !member.getFullname().isEmpty()
                    ? member.getFullname()
                    : member.getEmail();
            tvAssigneeName.setText(displayName);

            btnRemoveAssignee.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveAssignee(member, getAdapterPosition());
                }
            });
        }
    }
}

