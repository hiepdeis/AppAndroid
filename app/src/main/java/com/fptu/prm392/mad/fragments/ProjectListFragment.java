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
import com.fptu.prm392.mad.GlobalSearchActivity;
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

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private FirebaseAuth mAuth;

    private OnProjectClickListener projectClickListener;

    // Realtime listener
    private com.google.firebase.firestore.ListenerRegistration projectsListener;

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
        EditText searchBar = view.findViewById(R.id.searchBar);
        LinearLayout searchBarContainer = view.findViewById(R.id.searchBarContainer);
        ImageView fabCreateProject = view.findViewById(R.id.fabCreateProject);
        ImageView btnJoinProject = view.findViewById(R.id.btnJoinProject);
        ImageView btnGlobalSearch = view.findViewById(R.id.btnGlobalSearch);

        // Setup RecyclerView
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        projectAdapter = new ProjectAdapter(currentUserId, project -> {
            if (projectClickListener != null) {
                projectClickListener.onProjectClick(project);
            }
        });
        recyclerViewProjects.setAdapter(projectAdapter);

        // Setup search bar container click - open Global Search
        searchBarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GlobalSearchActivity.class);
            startActivity(intent);
        });

        // Also setup Global Search button click
        btnGlobalSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GlobalSearchActivity.class);
            startActivity(intent);
        });

        // Setup Create Project button click
        fabCreateProject.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateProjectActivity.class);
            startActivity(intent);
        });

        // Setup Join Project button click
        btnJoinProject.setOnClickListener(v -> {
            showSearchProjectsDialog();
        });

        // Note: Search bar is now disabled for local filtering
        // All searches go through GlobalSearchActivity
        startListeningToProjects();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop listening when fragment is destroyed
        stopListeningToProjects();
    }

    private void startListeningToProjects() {
        // Setup realtime listener
        projectsListener = projectRepository.listenToMyProjects(
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

    private void stopListeningToProjects() {
        if (projectsListener != null) {
            projectsListener.remove();
            projectsListener = null;
        }
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
        // Realtime listener đã active, không cần reload
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

        // Load all projects, excluding projects user already joined
        projectRepository.getMyProjects(
            myProjects -> {
                // Get list of project IDs user already joined
                List<String> myProjectIds = new ArrayList<>();
                for (Project p : myProjects) {
                    myProjectIds.add(p.getProjectId());
                }

                // Load all projects and filter out joined ones
                projectRepository.getAllProjects(
                    allProjects -> {
                        // Filter out projects user already joined
                        List<Project> availableProjects = new ArrayList<>();
                        for (Project project : allProjects) {
                            if (!myProjectIds.contains(project.getProjectId())) {
                                availableProjects.add(project);
                            }
                        }

                        if (availableProjects.isEmpty()) {
                            rvSearchProjects.setVisibility(View.GONE);
                            tvEmptyProjects.setVisibility(View.VISIBLE);
                            tvEmptyProjects.setText("No available projects to join");
                        } else {
                            rvSearchProjects.setVisibility(View.VISIBLE);
                            tvEmptyProjects.setVisibility(View.GONE);
                            searchAdapter.setProjects(availableProjects);
                        }
                    },
                    e -> {
                        Toast.makeText(getContext(), "Error loading projects: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                Toast.makeText(getContext(), "Error loading your projects: " + e.getMessage(),
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
        String currentUserEmail = mAuth.getCurrentUser().getEmail();

        // Check if already sent request
        com.fptu.prm392.mad.repositories.ProjectJoinRequestRepository requestRepo =
            new com.fptu.prm392.mad.repositories.ProjectJoinRequestRepository();

        requestRepo.hasPendingRequest(project.getProjectId(), currentUserId,
            hasPending -> {
                if (hasPending) {
                    Toast.makeText(getContext(), "You already sent a join request for this project", Toast.LENGTH_LONG).show();
                    return;
                }

                // Get current user info first
                com.fptu.prm392.mad.repositories.UserRepository userRepository =
                    new com.fptu.prm392.mad.repositories.UserRepository();
                userRepository.getUserById(currentUserId,
                    user -> {
                        // Create Join Request (user xin vào)
                        com.fptu.prm392.mad.models.ProjectJoinRequest request =
                            new com.fptu.prm392.mad.models.ProjectJoinRequest(
                                null, // requestId will be generated
                                project.getProjectId(),
                                project.getName(),
                                currentUserId,
                                user.getFullname(),
                                currentUserEmail,
                                user.getAvatar(),
                                project.getCreatedBy(), // managerId (người nhận request)
                                "join_request" // user xin vào project
                            );

                        // Send request
                        requestRepo.createJoinRequest(request,
                            requestId -> {
                                Toast.makeText(getContext(), "Join request sent! Waiting for manager approval.",
                                    Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            },
                            e -> {
                                Toast.makeText(getContext(), "Error sending request: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            }
                        );
                    },
                    e -> {
                        Toast.makeText(getContext(), "Error loading user info: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                Toast.makeText(getContext(), "Error checking request: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }
}

