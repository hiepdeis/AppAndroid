package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projects;
    private List<Integer> myTodoCounts; // Số lượng todo tasks của current user cho mỗi project
    private String currentUserId;
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectAdapter(String currentUserId, OnProjectClickListener listener) {
        this.projects = new ArrayList<>();
        this.myTodoCounts = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
        this.myTodoCounts = new ArrayList<>();
        // Khởi tạo myTodoCounts với giá trị 0, sẽ được update sau
        for (int i = 0; i < projects.size(); i++) {
            this.myTodoCounts.add(0);
        }
        notifyDataSetChanged();
    }

    public void updateMyTodoCount(int position, int count) {
        if (position >= 0 && position < myTodoCounts.size()) {
            myTodoCounts.set(position, count);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        int myTodoCount = myTodoCounts.get(position);
        holder.bind(project, myTodoCount, currentUserId);
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectName, tvRole, tvMyTodoCount, tvMemberCount, tvTaskCount;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvMyTodoCount = itemView.findViewById(R.id.tvMyTodoCount);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
        }

        public void bind(Project project, int myTodoCount, String currentUserId) {
            tvProjectName.setText(project.getName());

            // Xác định role: owner hoặc member
            boolean isOwner = project.getCreatedBy().equals(currentUserId);
            if (isOwner) {
                tvRole.setText("Owner");
                tvRole.setBackgroundResource(R.drawable.bg_status_done); // Xanh lá
            } else {
                tvRole.setText("Member");
                tvRole.setBackgroundResource(R.drawable.bg_status_in_progress); // Vàng
            }

            // Hiển thị số lượng todo tasks của user hiện tại
            tvMyTodoCount.setText(String.valueOf(myTodoCount));

            // Hiển thị stats (chỉ số, không có emoji vì emoji đã có trong layout)
            tvMemberCount.setText(String.valueOf(project.getMemberCount()));
            tvTaskCount.setText(String.valueOf(project.getTaskCount()));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });
        }
    }
}

