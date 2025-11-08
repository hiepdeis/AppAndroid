package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.ProjectAdapter;
import com.fptu.prm392.mad.adapters.TaskListAdapter;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogout;
    private ImageView fabCreateProject;
    private BottomNavigationView bottomNavigationView;

    // Containers
    private FrameLayout contentArea;
    private LinearLayout projectsContainer, taskContainer, otherTabsContainer, emptyState, emptyStateTask;
    private ScrollView profileContainer;

    // Projects tab
    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private EditText searchBar;

    // Tasks tab
    private RecyclerView recyclerViewTasks;
    private TaskListAdapter taskListAdapter;

    // Repositories
    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;

    private TextView tvTabMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        projectRepository = new ProjectRepository();
        taskRepository = new TaskRepository();

        // Initialize views
        btnLogout = findViewById(R.id.btnLogout);
        fabCreateProject = findViewById(R.id.fabCreateProject);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        contentArea = findViewById(R.id.contentArea);
        projectsContainer = findViewById(R.id.projectsContainer);
        taskContainer = findViewById(R.id.taskContainer);
        otherTabsContainer = findViewById(R.id.otherTabsContainer);
        profileContainer = findViewById(R.id.profileContainer);
        emptyState = findViewById(R.id.emptyState);
        emptyStateTask = findViewById(R.id.emptyStateTask);
        recyclerViewProjects = findViewById(R.id.recyclerViewProjects);
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        tvTabMessage = findViewById(R.id.tvTabMessage);
        searchBar = findViewById(R.id.searchBar);

        // Force disable icon tint để hiển thị màu gốc của PNG
        bottomNavigationView.setItemIconTintList(null);

        // Lấy thông tin user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Setup RecyclerView for Projects
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter = new ProjectAdapter(currentUserId, project -> {
            // Mở chi tiết project
            Intent intent = new Intent(HomeActivity.this, ProjectDetailActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        });
        recyclerViewProjects.setAdapter(projectAdapter);

        // Setup RecyclerView for Tasks
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        taskListAdapter = new TaskListAdapter(task -> {
            // TODO: Open task detail when clicked
            Toast.makeText(HomeActivity.this, "Task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewTasks.setAdapter(taskListAdapter);

        // Tự động chọn tab Project khi vào màn hình
        bottomNavigationView.setSelectedItemId(R.id.nav_project);
        showProjectsTab();

        // Xử lý sự kiện click bottom navigation
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_project) {
                    showProjectsTab();
                    return true;
                } else if (itemId == R.id.nav_task) {
                    showTaskTab();
                    return true;
                } else if (itemId == R.id.nav_calendar) {
                    showOtherTab("Calendar");
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    showOtherTab("Chat");
                    return true;
                } else if (itemId == R.id.nav_notification) {
                    showOtherTab("Notifications");
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    showProfileTab();
                    return true;
                }
                return false;
            }
        });

        // Xử lý đăng xuất
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(HomeActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });

        // Xử lý tạo dự án mới
        fabCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, CreateProjectActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh projects list khi quay lại activity
        if (projectsContainer.getVisibility() == View.VISIBLE) {
            loadProjects();
        } else if (profileContainer.getVisibility() == View.VISIBLE) {
            loadUserProfile();
        }
    }

    private void showProjectsTab() {
        projectsContainer.setVisibility(View.VISIBLE);
        taskContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.VISIBLE);
        contentArea.setBackgroundResource(R.drawable.img_3);
        loadProjects();
    }

    private void showTaskTab() {
        projectsContainer.setVisibility(View.GONE);
        taskContainer.setVisibility(View.VISIBLE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_2);
        loadTasks();
    }

    private void showOtherTab(String tabName) {
        projectsContainer.setVisibility(View.GONE);
        taskContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.VISIBLE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_3);
        tvTabMessage.setText(tabName + " - Coming soon...");
    }

    private void showProfileTab() {
        projectsContainer.setVisibility(View.GONE);
        taskContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.VISIBLE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.profile_backgrounf);
        loadUserProfile();
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
                Toast.makeText(this, "Error loading projects: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadTodoCountsForProjects(List<Project> projects) {
        String currentUserId = mAuth.getCurrentUser().getUid();

        for (int i = 0; i < projects.size(); i++) {
            final int position = i;
            Project project = projects.get(i);

            taskRepository.countMyPendingTasksInProject(
                project.getProjectId(),
                currentUserId,
                count -> {
                    projectAdapter.updateMyTodoCount(position, count);
                },
                e -> {
                    // Ignore error, giữ count = 0
                }
            );
        }
    }

    private void loadTasks() {
        taskRepository.getMyTasks(
            tasks -> {
                if (tasks.isEmpty()) {
                    emptyStateTask.setVisibility(View.VISIBLE);
                    recyclerViewTasks.setVisibility(View.GONE);
                } else {
                    emptyStateTask.setVisibility(View.GONE);
                    recyclerViewTasks.setVisibility(View.VISIBLE);
                    // Display tasks sorted by due date
                    taskListAdapter.setTasks(tasks);
                }
            },
            e -> {
                Toast.makeText(this, "Error loading tasks: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadUserProfile() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        UserRepository userRepository = new UserRepository();

        // Find profile views
        ImageView ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        TextView tvProfileFullname = findViewById(R.id.tvProfileFullname);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        Button btnProfileSignOut = findViewById(R.id.btnProfileSignOut);

        // Setup sign out button
        btnProfileSignOut.setOnClickListener(v -> showSignOutConfirmation());

        userRepository.getUserById(currentUserId,
            user -> {
                // Display user info
                if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                    tvProfileFullname.setText(user.getFullname());
                } else {
                    tvProfileFullname.setText("No name");
                }

                tvProfileEmail.setText(user.getEmail());
            },
            e -> {
                Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showSignOutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out", (dialog, which) -> {
                signOut();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void signOut() {
        // Sign out from Firebase
        mAuth.signOut();

        // Redirect to login screen
        navigateToLogin();

        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, com.fptu.prm392.mad.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
