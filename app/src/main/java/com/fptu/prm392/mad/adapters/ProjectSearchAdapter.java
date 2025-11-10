package com.fptu.prm392.mad.adapters;

import android.text.TextUtils;
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

public class ProjectSearchAdapter extends RecyclerView.Adapter<ProjectSearchAdapter.ProjectViewHolder> {

    private List<Project> projects;
    private List<Project> projectsFiltered;
    private OnProjectClickListener clickListener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectSearchAdapter(OnProjectClickListener listener) {
        this.projects = new ArrayList<>();
        this.projectsFiltered = new ArrayList<>();
        this.clickListener = listener;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
        this.projectsFiltered = new ArrayList<>(projects);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        projectsFiltered.clear();

        if (TextUtils.isEmpty(query)) {
            projectsFiltered.addAll(projects);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Project project : projects) {
                String name = project.getName() != null ? project.getName().toLowerCase() : "";
                String description = project.getDescription() != null ? project.getDescription().toLowerCase() : "";

                if (name.contains(lowerQuery) || description.contains(lowerQuery)) {
                    projectsFiltered.add(project);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_search, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectsFiltered.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectsFiltered.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private TextView tvProjectName;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
        }

        public void bind(Project project) {
            tvProjectName.setText(project.getName());

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onProjectClick(project);
                }
            });
        }
    }
}

