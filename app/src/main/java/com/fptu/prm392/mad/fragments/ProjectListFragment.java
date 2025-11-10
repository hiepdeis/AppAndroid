package com.fptu.prm392.mad.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.CreateProjectActivity;
import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.ProjectAdapter;
import com.fptu.prm392.mad.adapters.ProjectSearchAdapter;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ProjectListFragment extends Fragment {

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private LinearLayout emptyState;
    private EditText searchBar;
    private ImageView fabCreateProject;
    private ImageView btnJoinProject;

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
        fabCreateProject = view.findViewById(R.id.fabCreateProject);
        btnJoinProject = view.findViewById(R.id.btnJoinProject);

        // Setup RecyclerView
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        projectAdapter = new ProjectAdapter(currentUserId, project -> {
            if (projectClickListener != null) {
                projectClickListener.onProjectClick(project);
            }
        });
        recyclerViewProjects.setAdapter(projectAdapter);

        // Setup Create Project button click
        fabCreateProject.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateProjectActivity.class);
            startActivity(intent);
        });

        // Setup Join Project button click
        btnJoinProject.setOnClickListener(v -> {
            showSearchProjectsDialog();
        });

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

    private void showSearchProjectsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_search_projects);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Find views
        ImageView btnBack = dialog.findViewById(R.id.btnBack);
        EditText etSearchProjects = dialog.findViewById(R.id.etSearchProjects);
        RecyclerView rvSearchProjects = dialog.findViewById(R.id.rvSearchProjects);
        TextView tvEmptyProjects = dialog.findViewById(R.id.tvEmptyProjects);

        // Setup RecyclerView
        rvSearchProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        ProjectSearchAdapter searchAdapter = new ProjectSearchAdapter(project -> {
            // Show project detail mini dialog
            showProjectDetailMiniDialog(project, dialog);
        });
        rvSearchProjects.setAdapter(searchAdapter);

        // Load all projects
        projectRepository.getAllProjects(
            projects -> {
                if (projects.isEmpty()) {
                    rvSearchProjects.setVisibility(View.GONE);
                    tvEmptyProjects.setVisibility(View.VISIBLE);
                } else {
                    rvSearchProjects.setVisibility(View.VISIBLE);
                    tvEmptyProjects.setVisibility(View.GONE);
                    searchAdapter.setProjects(projects);
                }
            },
            e -> {
                Toast.makeText(getContext(), "Error loading projects: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );

        // Setup search
        etSearchProjects.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Back button
        btnBack.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showProjectDetailMiniDialog(Project project, Dialog parentDialog) {
        if (getContext() == null) return;

        Dialog detailDialog = new Dialog(requireContext());
        detailDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        detailDialog.setContentView(R.layout.dialog_project_detail_mini);

        if (detailDialog.getWindow() != null) {
            detailDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            detailDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Find views
        TextView tvProjectName = detailDialog.findViewById(R.id.tvProjectName);
        TextView tvCreatedDate = detailDialog.findViewById(R.id.tvCreatedDate);
        TextView tvDescription = detailDialog.findViewById(R.id.tvDescription);
        TextView tvMemberCount = detailDialog.findViewById(R.id.tvMemberCount);
        TextView tvTaskCount = detailDialog.findViewById(R.id.tvTaskCount);
        com.google.android.material.button.MaterialButton btnCancel = detailDialog.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnJoinProject = detailDialog.findViewById(R.id.btnJoinProject);

        // Set project info
        tvProjectName.setText(project.getName());

        // Format created date
        if (project.getCreatedAt() != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            tvCreatedDate.setText("Created: " + dateFormat.format(project.getCreatedAt()));
        } else {
            tvCreatedDate.setText("Created: N/A");
        }

        // Set description
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            tvDescription.setText(project.getDescription());
        } else {
            tvDescription.setText("No description available");
        }

        // Set stats
        tvMemberCount.setText(String.valueOf(project.getMemberCount()));
        tvTaskCount.setText(String.valueOf(project.getTaskCount()));

        // Cancel button
        btnCancel.setOnClickListener(v -> detailDialog.dismiss());

        // Join button
        btnJoinProject.setOnClickListener(v -> {
            detailDialog.dismiss();
            joinProject(project, parentDialog);
        });

        detailDialog.show();
    }

    private void joinProject(Project project, Dialog dialog) {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Get current user info first
        com.fptu.prm392.mad.repositories.UserRepository userRepository = new com.fptu.prm392.mad.repositories.UserRepository();
        userRepository.getUserById(currentUserId,
            user -> {
                // Create ProjectMember object with user info
                com.fptu.prm392.mad.models.ProjectMember member = new com.fptu.prm392.mad.models.ProjectMember(
                    project.getProjectId(),
                    user.getUserId(),
                    user.getFullname(),
                    user.getEmail(),
                    user.getAvatar(),
                    "member" // Default role
                );

                // Add current user to project members
                projectRepository.addMemberToProject(
                    project.getProjectId(),
                    member,
                    aVoid -> {
                        Toast.makeText(getContext(), "Joined project: " + project.getName(),
                            Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadProjects(); // Refresh project list
                    },
                    e -> {
                        Toast.makeText(getContext(), "Error joining project: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                Toast.makeText(getContext(), "Error loading user info: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }
}

