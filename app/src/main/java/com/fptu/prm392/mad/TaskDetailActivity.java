package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.SelectedAssigneeAdapter;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.ChatRepository;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskDetailActivity extends AppCompatActivity {

    private ImageView btnBack, btnAddAssignee;
    private TextView tvTaskTitle, tvTaskStatus, tvTaskDescription;
    private TextView tvCreatedBy, tvDueDate, tvTimeLeft, tvAssigneeCount;
    private RecyclerView rvAssignees;
    private MaterialButton btnChat, btnDelete;

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;

    private String taskId;
    private Task currentTask;
    private String taskCreatorId;
    private String currentUserId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private SelectedAssigneeAdapter assigneeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskRepository = new TaskRepository();
        userRepository = new UserRepository();
        projectRepository = new ProjectRepository();

        // Get current user ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get taskId from intent
        taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId == null) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnAddAssignee = findViewById(R.id.btnAddAssignee);
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskStatus = findViewById(R.id.tvTaskStatus);
        tvTaskDescription = findViewById(R.id.tvTaskDescription);
        tvCreatedBy = findViewById(R.id.tvCreatedBy);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvTimeLeft = findViewById(R.id.tvTimeLeft);
        tvAssigneeCount = findViewById(R.id.tvAssigneeCount);
        rvAssignees = findViewById(R.id.rvAssignees);
        btnChat = findViewById(R.id.btnChat);
        btnDelete = findViewById(R.id.btnDelete);

        // Setup RecyclerView - Vertical list
        rvAssignees.setLayoutManager(new LinearLayoutManager(this));
        assigneeAdapter = new SelectedAssigneeAdapter(
                (member, position) -> showRemoveAssigneeConfirmation(member),
                member -> openUserProfile(member)
        );
        rvAssignees.setAdapter(assigneeAdapter);

        // Setup listeners
        btnBack.setOnClickListener(v -> finish());

        btnAddAssignee.setOnClickListener(v -> showAddAssigneeDialog());

        // Click status badge to change status
        tvTaskStatus.setOnClickListener(v -> showStatusChangeDialog());

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        btnChat.setOnClickListener(v -> openTaskChat());

        // Load task details
        loadTaskDetails();
    }

    private void loadTaskDetails() {
        // We need to query Firestore to get task by ID
        taskRepository.getTaskById(taskId,
                task -> {
                    currentTask = task;
                    taskCreatorId = task.getCreatedBy();
                    displayTaskDetails();
                    loadCreatorInfo();
                    loadAssignees();
                    updateUIBasedOnOwnership();
                },
                e -> {
                    Toast.makeText(this, "Error loading task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
    }

    private void updateUIBasedOnOwnership() {
        boolean isCreator = currentUserId.equals(taskCreatorId);

        // Hiện/ẩn nút add assignee
        btnAddAssignee.setVisibility(isCreator ? View.VISIBLE : View.GONE);

        // Set flag cho adapter để enable/disable delete buttons
        assigneeAdapter.setIsCreator(isCreator);
    }

    private void displayTaskDetails() {
        if (currentTask == null) return;

        // Set title
        tvTaskTitle.setText(currentTask.getTitle());

        // Set status with appropriate styling
        String status = currentTask.getStatus();
        if ("todo".equals(status)) {
            tvTaskStatus.setText("To Do");
            tvTaskStatus.setBackgroundResource(R.drawable.status_badge_todo);
        } else if ("in_progress".equals(status)) {
            tvTaskStatus.setText("In Progress");
            tvTaskStatus.setBackgroundResource(R.drawable.status_badge_in_progress);
        } else if ("done".equals(status)) {
            tvTaskStatus.setText("Done");
            tvTaskStatus.setBackgroundResource(R.drawable.status_badge_done);
        }

        // Set description
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            tvTaskDescription.setText(currentTask.getDescription());
        } else {
            tvTaskDescription.setText("No description");
        }

        // Set due date and calculate time left
        if (currentTask.getDueDate() != null) {
            Date dueDate = currentTask.getDueDate().toDate();
            tvDueDate.setText(dateFormat.format(dueDate));

            // Calculate time left
            String timeLeft = calculateTimeLeft(dueDate);
            if (timeLeft != null) {
                tvTimeLeft.setText("(" + timeLeft + " left)");
                tvTimeLeft.setVisibility(View.VISIBLE);

                // Change color based on urgency
                long daysLeft = TimeUnit.MILLISECONDS.toDays(dueDate.getTime() - System.currentTimeMillis());
                if (daysLeft < 0) {
                    tvTimeLeft.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                    tvTimeLeft.setText("(Overdue)");
                } else if (daysLeft <= 2) {
                    tvTimeLeft.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, null));
                } else {
                    tvTimeLeft.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                }
            }
        } else {
            tvDueDate.setText("No due date");
            tvTimeLeft.setVisibility(View.GONE);
        }
    }

    private String calculateTimeLeft(Date dueDate) {
        long currentTime = System.currentTimeMillis();
        long dueTime = dueDate.getTime();
        long diff = dueTime - currentTime;

        if (diff < 0) {
            // Overdue
            return null;
        }

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;

        if (days > 0) {
            if (hours > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " " + hours + " hour" + (hours > 1 ? "s" : "");
            }
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }
    }

    private void loadCreatorInfo() {
        if (currentTask == null || currentTask.getCreatedBy() == null) return;

        userRepository.getUserById(currentTask.getCreatedBy(),
                user -> {
                    String displayName = user.getFullname();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = user.getEmail();
                    }
                    tvCreatedBy.setText(displayName);
                },
                e -> {
                    tvCreatedBy.setText("Unknown");
                }
        );
    }

    private void loadAssignees() {
        if (currentTask == null || currentTask.getAssignees() == null || currentTask.getAssignees().isEmpty()) {
            tvAssigneeCount.setText(" (0)");
            return;
        }

        // Update count
        int count = currentTask.getAssignees().size();
        tvAssigneeCount.setText(" (" + count + ")");

        // Load project members to get full info
        projectRepository.getProjectMembers(currentTask.getProjectId(),
                members -> {
                    // Filter members who are assignees
                    List<ProjectMember> assignees = new ArrayList<>();
                    for (ProjectMember member : members) {
                        if (currentTask.getAssignees().contains(member.getUserId())) {
                            assignees.add(member);
                        }
                    }
                    assigneeAdapter.setSelectedMembers(assignees);
                },
                e -> {
                    Toast.makeText(this, "Error loading assignees: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showStatusChangeDialog() {
        if (currentTask == null) return;

        String[] statusOptions = {"To Do", "In Progress", "Done"};
        String[] statusValues = {"todo", "in_progress", "done"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Change Task Status")
                .setItems(statusOptions, (dialog, which) -> {
                    String newStatus = statusValues[which];
                    if (!newStatus.equals(currentTask.getStatus())) {
                        updateTaskStatus(newStatus);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTaskStatus(String newStatus) {
        taskRepository.updateTaskStatus(taskId, newStatus,
                aVoid -> {
                    Toast.makeText(this, "Status updated successfully!", Toast.LENGTH_SHORT).show();
                    // Reload task details to reflect the change
                    loadTaskDetails();
                },
                e -> {
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask() {
        taskRepository.deleteTask(taskId,
                aVoid -> {
                    Toast.makeText(this, "Task deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> {
                    Toast.makeText(this, "Failed to delete task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showAddAssigneeDialog() {
        if (currentTask == null) return;

        // Create dialog using existing layout
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_users);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views
        EditText etSearchUser = dialog.findViewById(R.id.etSearchUser);
        RecyclerView rvUsers = dialog.findViewById(R.id.rvUsers);
        TextView tvEmptyUsers = dialog.findViewById(R.id.tvEmptyUsers);

        // Setup RecyclerView
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        com.fptu.prm392.mad.adapters.AddUserAdapter addUserAdapter =
                new com.fptu.prm392.mad.adapters.AddUserAdapter((com.fptu.prm392.mad.adapters.AddUserAdapter.OnAddUserListener) user -> {
            addAssigneeToTask(user, dialog);
        });
        rvUsers.setAdapter(addUserAdapter);

        // Load available users (project members who are not assignees yet)
        loadAvailableAssignees(addUserAdapter, rvUsers, tvEmptyUsers);

        // Search functionality
        etSearchUser.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                addUserAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        dialog.show();
    }

    private void loadAvailableAssignees(com.fptu.prm392.mad.adapters.AddUserAdapter adapter,
                                        RecyclerView recyclerView, TextView emptyView) {
        // Get all project members
        projectRepository.getProjectMembers(currentTask.getProjectId(),
                members -> {
                    // Filter out already assigned members
                    List<com.fptu.prm392.mad.models.User> availableUsers = new ArrayList<>();
                    List<String> currentAssignees = currentTask.getAssignees() != null
                            ? currentTask.getAssignees() : new ArrayList<>();

                    for (ProjectMember member : members) {
                        if (!currentAssignees.contains(member.getUserId())) {
                            // Convert ProjectMember to User
                            com.fptu.prm392.mad.models.User user = new com.fptu.prm392.mad.models.User();
                            user.setUserId(member.getUserId());
                            user.setFullname(member.getFullname());
                            user.setEmail(member.getEmail());
                            availableUsers.add(user);
                        }
                    }

                    if (availableUsers.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("All project members are already assigned");
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        adapter.setUsers(availableUsers);
                    }
                },
                e -> Toast.makeText(this, "Error loading members: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void addAssigneeToTask(com.fptu.prm392.mad.models.User user, android.app.Dialog dialog) {
        taskRepository.addAssigneeToTask(taskId, user.getUserId(),
                aVoid -> {
                    Toast.makeText(this, "Assignee added successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadTaskDetails(); // Reload to update assignees
                },
                e -> Toast.makeText(this, "Error adding assignee: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void showRemoveAssigneeConfirmation(ProjectMember member) {
        String displayName = member.getFullname() != null && !member.getFullname().isEmpty()
                ? member.getFullname() : member.getEmail();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remove Assignee")
                .setMessage("Remove " + displayName + " from this task?")
                .setPositiveButton("Remove", (dialog, which) -> removeAssigneeFromTask(member.getUserId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeAssigneeFromTask(String userId) {
        taskRepository.removeAssigneeFromTask(taskId, userId,
                aVoid -> {
                    Toast.makeText(this, "Assignee removed successfully", Toast.LENGTH_SHORT).show();
                    loadTaskDetails(); // Reload to update assignees
                },
                e -> Toast.makeText(this, "Error removing assignee: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void openTaskChat() {
        if (currentTask == null) {
            Toast.makeText(this, "Loading task...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get assignees + creator
        List<String> participantIds = new ArrayList<>();
        if (currentTask.getAssignees() != null) {
            participantIds.addAll(currentTask.getAssignees());
        }

        // Add creator if not already in list
        if (!participantIds.contains(currentTask.getCreatedBy())) {
            participantIds.add(currentTask.getCreatedBy());
        }

        // Add current user if not already in list
        if (!participantIds.contains(currentUserId)) {
            participantIds.add(currentUserId);
        }

        if (participantIds.size() < 2) {
            Toast.makeText(this, "Need at least 2 people to start a chat", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create chat name from task title
        String chatName = "Task: " + currentTask.getTitle();

        // Create or get task chat (using task ID as identifier)
        ChatRepository chatRepository = new ChatRepository();
        chatRepository.getOrCreateProjectChat(
                taskId, // Use taskId as unique identifier
                chatName,
                participantIds,
                chat -> {
                    // Navigate to HomeActivity with chat tab
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("OPEN_CHAT_ID", chat.getChatId());
                    intent.putExtra("CHAT_NAME", chat.getProjectName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                },
                e -> Toast.makeText(this, "Error creating chat: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void openUserProfile(ProjectMember member) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", member.getUserId());
        if (currentTask != null && currentTask.getProjectId() != null) {
            intent.putExtra("PROJECT_ID", currentTask.getProjectId());
        }
        startActivity(intent);
    }
}

