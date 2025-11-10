package com.fptu.prm392.mad;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.AddUserAdapter;
import com.fptu.prm392.mad.adapters.MemberAdapter;
import com.fptu.prm392.mad.adapters.TaskCardAdapter;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class ProjectDetailActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvProjectName, tvProjectDescription, tvMemberCount, tvTaskStats;

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private String projectId;
    private Project currentProject;

    // Kanban Board
    private RecyclerView rvTodoTasks, rvInProgressTasks, rvDoneTasks;
    private TaskCardAdapter todoAdapter, inProgressAdapter, doneAdapter;

    // Horizontal Menu
    private ImageView fabMain;
    private LinearLayout horizontalMenu;
    private LinearLayout menuMember, menuTask, menuChat;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        projectRepository = new ProjectRepository();
        taskRepository = new TaskRepository();
        userRepository = new UserRepository();

        // Get projectId from intent
        projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null) {
            Toast.makeText(this, "Error: Project not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        tvProjectName = findViewById(R.id.tvProjectName);
        tvProjectDescription = findViewById(R.id.tvProjectDescription);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvTaskStats = findViewById(R.id.tvTaskStats);
        // Initialize Kanban Board RecyclerViews
        rvTodoTasks = findViewById(R.id.rvTodoTasks);
        rvInProgressTasks = findViewById(R.id.rvInProgressTasks);
        rvDoneTasks = findViewById(R.id.rvDoneTasks);

        // Setup Kanban Board
        setupKanbanBoard();

        // Initialize Horizontal Menu
        fabMain = findViewById(R.id.fabMain);
        horizontalMenu = findViewById(R.id.horizontalMenu);
        menuMember = findViewById(R.id.menuMember);
        menuTask = findViewById(R.id.menuTask);
        menuChat = findViewById(R.id.menuChat);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Setup menu click listeners
        setupHorizontalMenu();

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isMenuOpen) {
                    closeMenu();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Load project details
        loadProjectDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload tasks when returning from CreateTaskActivity
        if (projectId != null) {
            loadTaskStatistics();
            loadTasks();
        }
    }

    private void loadProjectDetails() {
        projectRepository.getProjectById(projectId,
            project -> {
                currentProject = project;
                displayProjectInfo();
                loadTaskStatistics();
                loadTasks();
            },
            e -> {
                Toast.makeText(this, "Error loading project: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                finish();
            }
        );
    }

    private void displayProjectInfo() {
        if (currentProject != null) {
            tvProjectName.setText(currentProject.getName());

            // Show description if available
            if (currentProject.getDescription() != null && !currentProject.getDescription().isEmpty()) {
                tvProjectDescription.setText(currentProject.getDescription());
            } else {
                tvProjectDescription.setText("No description");
            }

            tvMemberCount.setText(String.valueOf(currentProject.getMemberCount()));
        }
    }

    private void loadTaskStatistics() {
        taskRepository.getProjectTaskStats(projectId,
            (pendingCount, doneCount, totalCount) -> {
                // Format: "doneCount/totalCount"
                // e.g., "5/10" means 5 done out of 10 total tasks
                String statsText = doneCount + "/" + totalCount;
                tvTaskStats.setText(statsText);
            },
            e -> {
                Toast.makeText(this, "Error loading task stats: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                tvTaskStats.setText("0/0");
            }
        );
    }

    private void setupKanbanBoard() {
        // Drag listener for when drag starts
        TaskCardAdapter.OnTaskDragListener dragListener = task -> {
            // Optional: Add visual feedback when drag starts
        };

        // Setup To Do column
        rvTodoTasks.setLayoutManager(new LinearLayoutManager(this));
        todoAdapter = new TaskCardAdapter(task -> {
            openTaskDetail(task);
        }, dragListener);
        rvTodoTasks.setAdapter(todoAdapter);
        setupDropZone(rvTodoTasks, "todo");

        // Setup In Progress column
        rvInProgressTasks.setLayoutManager(new LinearLayoutManager(this));
        inProgressAdapter = new TaskCardAdapter(task -> {
            openTaskDetail(task);
        }, dragListener);
        rvInProgressTasks.setAdapter(inProgressAdapter);
        setupDropZone(rvInProgressTasks, "in_progress");

        // Setup Done column
        rvDoneTasks.setLayoutManager(new LinearLayoutManager(this));
        doneAdapter = new TaskCardAdapter(task -> {
            openTaskDetail(task);
        }, dragListener);
        rvDoneTasks.setAdapter(doneAdapter);
        setupDropZone(rvDoneTasks, "done");
    }

    private void setupDropZone(RecyclerView recyclerView, String targetStatus) {
        recyclerView.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case android.view.DragEvent.ACTION_DRAG_ENTERED:
                    // Visual feedback when drag enters this column
                    v.setAlpha(0.7f);
                    return true;

                case android.view.DragEvent.ACTION_DRAG_EXITED:
                    // Remove visual feedback when drag exits
                    v.setAlpha(1.0f);
                    return true;

                case android.view.DragEvent.ACTION_DROP:
                    // Remove visual feedback
                    v.setAlpha(1.0f);

                    // Get the task from local state
                    Task droppedTask = (Task) event.getLocalState();

                    if (droppedTask != null && !droppedTask.getStatus().equals(targetStatus)) {
                        // Update task status in Firebase
                        updateTaskStatus(droppedTask, targetStatus);
                    }
                    return true;

                case android.view.DragEvent.ACTION_DRAG_ENDED:
                    // Remove visual feedback
                    v.setAlpha(1.0f);
                    return true;

                default:
                    return false;
            }
        });
    }

    private void updateTaskStatus(Task task, String newStatus) {
        // Show loading state
        Toast.makeText(this, "Moving task...", Toast.LENGTH_SHORT).show();

        taskRepository.updateTaskStatus(task.getTaskId(), newStatus,
            aVoid -> {
                // Success - reload tasks to reflect the change
                Toast.makeText(this, "Task moved successfully!", Toast.LENGTH_SHORT).show();
                loadTasks();
                loadTaskStatistics(); // Update stats as well
            },
            e -> {
                Toast.makeText(this, "Failed to move task: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadTasks() {
        taskRepository.getTasksByProject(projectId,
            tasks -> {
                // Separate tasks by status
                List<Task> todoTasks = new ArrayList<>();
                List<Task> inProgressTasks = new ArrayList<>();
                List<Task> doneTasks = new ArrayList<>();

                for (Task task : tasks) {
                    String status = task.getStatus();
                    if ("todo".equals(status)) {
                        todoTasks.add(task);
                    } else if ("in_progress".equals(status)) {
                        inProgressTasks.add(task);
                    } else if ("done".equals(status)) {
                        doneTasks.add(task);
                    }
                }

                // Update adapters
                todoAdapter.setTasks(todoTasks);
                inProgressAdapter.setTasks(inProgressTasks);
                doneAdapter.setTasks(doneTasks);
            },
            e -> {
                Toast.makeText(this, "Error loading tasks: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void setupHorizontalMenu() {
        // Main FAB click - toggle menu
        fabMain.setOnClickListener(v -> {
            if (isMenuOpen) {
                closeMenu();
            } else {
                openMenu();
            }
        });

        // Menu item clicks
        menuMember.setOnClickListener(v -> {
            openMemberListActivity();
            closeMenu();
        });

        menuTask.setOnClickListener(v -> {
            openCreateTaskActivity();
            closeMenu();
        });

        menuChat.setOnClickListener(v -> {
            openProjectChat();
            closeMenu();
        });
    }

    private void openMenu() {
        isMenuOpen = true;

        // Show and slide menu from right to left
        horizontalMenu.setVisibility(View.VISIBLE);
        horizontalMenu.setAlpha(0f);
        horizontalMenu.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void closeMenu() {
        isMenuOpen = false;

        // Slide menu back to right and hide
        horizontalMenu.animate()
                .translationX(400f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> horizontalMenu.setVisibility(View.GONE))
                .start();
    }

    private void openMemberListActivity() {
        Intent intent = new Intent(this, MemberListActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        startActivity(intent);
    }

    private void openCreateTaskActivity() {
        Intent intent = new Intent(this, CreateTaskActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        startActivity(intent);
    }

    private void openProjectChat() {
        if (currentProject == null) {
            Toast.makeText(this, "Loading project...", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement chat feature in HomeActivity
        Toast.makeText(this, "Chat feature will be available in Home screen", Toast.LENGTH_SHORT).show();
    }

    private void openTaskDetail(Task task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("TASK_ID", task.getTaskId());
        startActivity(intent);
    }

    private void showMembersDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_members);

        // Set dialog width
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize views
        RecyclerView rvMembers = dialog.findViewById(R.id.rvMembers);
        ImageView btnAddMember = dialog.findViewById(R.id.btnAddMember);

        // Setup RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        MemberAdapter memberAdapter = new MemberAdapter(new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onDeleteMember(ProjectMember member, int position) {
                // Show confirmation before deleting
                showDeleteMemberConfirmation(member, dialog);
            }
        });
        rvMembers.setAdapter(memberAdapter);

        // Load members
        loadProjectMembers(memberAdapter);

        // Add member button click
        btnAddMember.setOnClickListener(v -> {
            showAddMemberDialog(dialog);
        });

        dialog.show();
    }

    private void loadProjectMembers(MemberAdapter adapter) {
        projectRepository.getProjectMembers(projectId,
            members -> {
                adapter.setMembers(members);
            },
            e -> {
                Toast.makeText(this, "Error loading members: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showDeleteMemberConfirmation(ProjectMember member, Dialog parentDialog) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove " +
                (member.getFullname() != null ? member.getFullname() : member.getEmail()) +
                " from this project?")
            .setPositiveButton("Remove", (dialog, which) -> {
                deleteMember(member, parentDialog);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteMember(ProjectMember member, Dialog parentDialog) {
        projectRepository.removeMemberFromProject(projectId, member.getUserId(),
            aVoid -> {
                Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                // Reload members in dialog
                RecyclerView rvMembers = parentDialog.findViewById(R.id.rvMembers);
                if (rvMembers != null && rvMembers.getAdapter() instanceof MemberAdapter) {
                    loadProjectMembers((MemberAdapter) rvMembers.getAdapter());
                }
                // Reload project details to update member count
                loadProjectDetails();
            },
            e -> {
                Toast.makeText(this, "Error removing member: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showAddMemberDialog(Dialog parentDialog) {
        Dialog addDialog = new Dialog(this);
        addDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addDialog.setContentView(R.layout.dialog_add_users);

        // Set dialog width
        Window window = addDialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize views
        RecyclerView rvUsers = addDialog.findViewById(R.id.rvUsers);
        EditText etSearchUser = addDialog.findViewById(R.id.etSearchUser);
        TextView tvEmptyUsers = addDialog.findViewById(R.id.tvEmptyUsers);

        // Setup RecyclerView
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        AddUserAdapter addUserAdapter = new AddUserAdapter((AddUserAdapter.OnAddUserListener) user -> {
            // Add user to project
            addMemberToProject(user, addDialog, parentDialog);
        });
        rvUsers.setAdapter(addUserAdapter);

        // Load users excluding existing members
        loadAvailableUsers(addUserAdapter, rvUsers, tvEmptyUsers);

        // Search functionality
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                addUserAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addDialog.show();
    }

    private void loadAvailableUsers(AddUserAdapter adapter, RecyclerView recyclerView, TextView emptyView) {
        // First, get current project members
        projectRepository.getProjectMembers(projectId,
            members -> {
                // Get list of member user IDs
                List<String> memberUserIds = new ArrayList<>();
                for (ProjectMember member : members) {
                    memberUserIds.add(member.getUserId());
                }

                // Then get all users
                userRepository.getAllUsers(
                    allUsers -> {
                        // Filter out existing members
                        List<User> availableUsers = new ArrayList<>();
                        for (User user : allUsers) {
                            if (!memberUserIds.contains(user.getUserId())) {
                                availableUsers.add(user);
                            }
                        }

                        // Update UI
                        if (availableUsers.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                            adapter.setUsers(availableUsers);
                        }
                    },
                    e -> {
                        Toast.makeText(this, "Error loading users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                Toast.makeText(this, "Error loading members: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void addMemberToProject(User user, Dialog addDialog, Dialog parentDialog) {
        // Create ProjectMember object
        ProjectMember newMember = new ProjectMember(
            projectId,
            user.getUserId(),
            user.getFullname(),
            user.getEmail(),
            user.getAvatar(),
            "member" // Default role is member
        );

        // Add to Firestore
        projectRepository.addMemberToProject(projectId, newMember,
            aVoid -> {
                Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show();

                // Close add dialog
                addDialog.dismiss();

                // Reload members in parent dialog
                RecyclerView rvMembers = parentDialog.findViewById(R.id.rvMembers);
                if (rvMembers != null && rvMembers.getAdapter() instanceof MemberAdapter) {
                    loadProjectMembers((MemberAdapter) rvMembers.getAdapter());
                }

                // Reload project details to update member count
                loadProjectDetails();
            },
            e -> {
                Toast.makeText(this, "Error adding member: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }
}
