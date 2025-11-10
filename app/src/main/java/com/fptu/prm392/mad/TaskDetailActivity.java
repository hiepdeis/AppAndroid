package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.SelectedAssigneeAdapter;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskDetailActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTaskTitle, tvTaskStatus, tvTaskDescription;
    private TextView tvCreatedBy, tvDueDate, tvTimeLeft, tvAssigneeCount;
    private RecyclerView rvAssignees;
    private MaterialButton btnChat, btnDelete;

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;

    private String taskId;
    private Task currentTask;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private SelectedAssigneeAdapter assigneeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskRepository = new TaskRepository();
        userRepository = new UserRepository();
        projectRepository = new ProjectRepository();

        // Get taskId from intent
        taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId == null) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
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
        assigneeAdapter = new SelectedAssigneeAdapter(null, member -> {
            // Open user profile when clicked
            openUserProfile(member);
        });
        rvAssignees.setAdapter(assigneeAdapter);

        // Setup listeners
        btnBack.setOnClickListener(v -> finish());

        // Click status badge to change status
        tvTaskStatus.setOnClickListener(v -> showStatusChangeDialog());

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        btnChat.setOnClickListener(v -> {
            Toast.makeText(this, "Chat feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Load task details
        loadTaskDetails();
    }

    private void loadTaskDetails() {
        // We need to query Firestore to get task by ID
        taskRepository.getTaskById(taskId,
                task -> {
                    currentTask = task;
                    displayTaskDetails();
                    loadCreatorInfo();
                    loadAssignees();
                },
                e -> {
                    Toast.makeText(this, "Error loading task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
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

    private void openUserProfile(ProjectMember member) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("USER_ID", member.getUserId());
        if (currentTask != null && currentTask.getProjectId() != null) {
            intent.putExtra("PROJECT_ID", currentTask.getProjectId());
        }
        startActivity(intent);
    }
}

