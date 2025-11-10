package com.fptu.prm392.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.ProjectAdapter;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ProjectListFragment extends Fragment {

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private LinearLayout emptyState;
    private EditText searchBar;

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private FirebaseAuth mAuth;

    private OnProjectClickListener projectClickListener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public static ProjectListFragment newInstance() {
        return new ProjectListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectRepository = new ProjectRepository();
        taskRepository = new TaskRepository();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_list, container, false);

        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);
        emptyState = view.findViewById(R.id.emptyState);
        searchBar = view.findViewById(R.id.searchBar);

        // Setup RecyclerView
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        projectAdapter = new ProjectAdapter(currentUserId, project -> {
            if (projectClickListener != null) {
                projectClickListener.onProjectClick(project);
            }
        });
        recyclerViewProjects.setAdapter(projectAdapter);

        loadProjects();

        return view;
    }

    private void loadProjects() {
        projectRepository.getMyProjects(
                projects -> {
                    if (projects.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerViewProjects.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerViewProjects.setVisibility(View.VISIBLE);
                        projectAdapter.setProjects(projects);

                        // Load todo count cho từng project
                        loadTodoCountsForProjects(projects);
                    }
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading projects: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void loadTodoCountsForProjects(List<Project> projects) {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        for (int i = 0; i < projects.size(); i++) {
            final int position = i;
            Project project = projects.get(i);

            taskRepository.countMyPendingTasksInProject(
                    project.getProjectId(),
                    currentUserId,
                    count -> projectAdapter.updateMyTodoCount(position, count),
                    e -> {
                        // Ignore error, giữ count = 0
                    }
            );
        }
    }

    public void setOnProjectClickListener(OnProjectClickListener listener) {
        this.projectClickListener = listener;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProjects();
    }
}

