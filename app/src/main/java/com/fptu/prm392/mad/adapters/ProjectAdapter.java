package com.fptu.prm392.mad.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Project;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private Context context;
    private List<Project> projectList;
    private SimpleDateFormat dateFormat;
    private OnProjectClickListener listener;

    // Interface cho click listener
    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectAdapter(Context context, List<Project> projectList) {
        this.context = context;
        this.projectList = projectList;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    public ProjectAdapter(Context context, List<Project> projectList, OnProjectClickListener listener) {
        this.context = context;
        this.projectList = projectList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);

        holder.tvProjectName.setText(project.getName());
        holder.tvProjectDescription.setText(project.getDescription());
        holder.tvMemberCount.setText(project.getMemberCount() + " thành viên");
        holder.tvTaskCount.setText(project.getTaskCount() + " công việc");

        if (project.getCreatedAt() != null) {
            holder.tvCreatedDate.setText("Tạo: " + dateFormat.format(project.getCreatedAt()));
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick(project);
            }
        });
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvProjectName, tvProjectDescription, tvMemberCount, tvTaskCount, tvCreatedDate;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardProject);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvProjectDescription = itemView.findViewById(R.id.tvProjectDescription);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
        }
    }
}
