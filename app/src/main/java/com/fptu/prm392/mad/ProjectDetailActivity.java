package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.MemberSelectAdapter;
import com.fptu.prm392.mad.adapters.TaskAdapter;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ProjectDetailActivity extends AppCompatActivity {

    private TextView tvProjectName, tvProjectDescription, tvMemberCount, tvTaskCount;
    private Button btnCreateTask, btnAddMember;
    private FloatingActionButton fabCreateTask;
    private RecyclerView recyclerViewTasks;
    private ProgressBar progressBar;
    private View viewEmptyTasks;

    private String projectId;
    private String projectName;
    private String projectCreatorId; // ID của người tạo project

    private FirebaseAuth mAuth;
    private ProjectRepository projectRepo;
    private TaskRepository taskRepo;
    private UserRepository userRepo;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private List<User> projectMembers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        // Lấy projectId từ Intent
        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        if (projectId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy dự án", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo
        mAuth = FirebaseAuth.getInstance();
        projectRepo = new ProjectRepository();
        taskRepo = new TaskRepository();
        userRepo = new UserRepository();
        taskList = new ArrayList<>();

        // Ánh xạ views
        tvProjectName = findViewById(R.id.tvProjectName);
        tvProjectDescription = findViewById(R.id.tvProjectDescription);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvTaskCount = findViewById(R.id.tvTaskCount);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnAddMember = findViewById(R.id.btnAddMember);
        fabCreateTask = findViewById(R.id.fabCreateTask);
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        progressBar = findViewById(R.id.progressBar);
        viewEmptyTasks = findViewById(R.id.tvEmptyTasks);

        // Setup RecyclerView
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, taskList);
        recyclerViewTasks.setAdapter(taskAdapter);

        // Set task action listener
        taskAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onAddAssigneeClick(Task task, int position) {
                showAddAssigneeDialog(task, position);
            }

            @Override
            public void onRemoveAssigneeClick(Task task, String userId, int position) {
                removeAssigneeFromTask(task, userId, position);
            }

            @Override
            public void loadAssigneeInfo(String userId, TaskAdapter.AssigneeInfoCallback callback) {
                userRepo.getUserById(userId, callback::onUserLoaded, error -> callback.onUserLoaded(null));
            }

            @Override
            public void onStatusClick(Task task, int position) {
                showChangeStatusDialog(task, position);
            }
        });

        // Load project members
        loadProjectMembers();

        // Load project info
        loadProjectInfo();
        loadTasks();

        // Xử lý nút Tạo Task (Button)
        btnCreateTask.setOnClickListener(v -> openCreateTask());

        // Xử lý nút Tạo Task (FAB)
        fabCreateTask.setOnClickListener(v -> openCreateTask());

        // Xử lý nút Tuyển Member
        btnAddMember.setOnClickListener(v -> showAddMemberToProjectDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload khi quay lại
        loadProjectInfo();
        loadTasks();
    }

    private void loadProjectMembers() {
        projectRepo.getProjectMembersWithDetails(projectId,
            members -> {
                projectMembers.clear();
                projectMembers.addAll(members);
            },
            error -> {
                Toast.makeText(this, "Lỗi tải danh sách thành viên", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadProjectInfo() {
        progressBar.setVisibility(View.VISIBLE);

        projectRepo.getProjectById(projectId,
            project -> {
                progressBar.setVisibility(View.GONE);

                projectCreatorId = project.getCreatedBy();

                // Hiển thị thông tin project
                tvProjectName.setText(project.getName());
                tvProjectDescription.setText(project.getDescription());
                tvMemberCount.setText(project.getMemberCount() + " thành viên");
                tvTaskCount.setText(project.getTaskCount() + " công việc");

                // Kiểm tra quyền: Chỉ owner mới thấy nút "Tuyển Member" và có thể đổi status
                String currentUserId = mAuth.getCurrentUser().getUid();
                boolean isOwner = currentUserId.equals(projectCreatorId);

                btnAddMember.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                taskAdapter.setCanEditTask(isOwner);
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải thông tin dự án: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadTasks() {
        progressBar.setVisibility(View.VISIBLE);

        taskRepo.getTasksByProject(projectId,
            tasks -> {
                progressBar.setVisibility(View.GONE);

                if (tasks.isEmpty()) {
                    viewEmptyTasks.setVisibility(View.VISIBLE);
                    recyclerViewTasks.setVisibility(View.GONE);
                } else {
                    viewEmptyTasks.setVisibility(View.GONE);
                    recyclerViewTasks.setVisibility(View.VISIBLE);

                    // Cập nhật danh sách tasks
                    taskList.clear();
                    taskList.addAll(tasks);
                    taskAdapter.notifyDataSetChanged();

                    Toast.makeText(this, "Tải " + tasks.size() + " công việc",
                        Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                viewEmptyTasks.setVisibility(View.VISIBLE);
                recyclerViewTasks.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải danh sách task", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showAddAssigneeDialog(Task task, int position) {
        if (projectMembers.isEmpty()) {
            Toast.makeText(this, "Chưa có thành viên trong dự án", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_members, null);
        builder.setView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewMembers);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Lấy current assignees
        List<String> currentAssignees = task.getAssignees() != null ?
                new ArrayList<>(task.getAssignees()) : new ArrayList<>();

        MemberSelectAdapter adapter = new MemberSelectAdapter(this, projectMembers, currentAssignees);
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            List<String> newAssignees = adapter.getSelectedMemberIds();
            updateTaskAssignees(task, newAssignees, position);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateTaskAssignees(Task task, List<String> newAssignees, int position) {
        progressBar.setVisibility(View.VISIBLE);

        task.setAssignees(newAssignees);

        taskRepo.updateTask(task,
            aVoid -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();

                // Cập nhật UI
                taskList.set(position, task);
                taskAdapter.notifyItemChanged(position);
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void removeAssigneeFromTask(Task task, String userId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận");
        builder.setMessage("Bạn có chắc muốn xóa người này khỏi task?");

        builder.setPositiveButton("Xóa", (dialog, which) -> {
            progressBar.setVisibility(View.VISIBLE);

            List<String> assignees = task.getAssignees();
            if (assignees != null) {
                assignees.remove(userId);
                task.setAssignees(assignees);

                taskRepo.updateTask(task,
                    aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Đã xóa người tham gia", Toast.LENGTH_SHORT).show();

                        // Cập nhật UI
                        taskList.set(position, task);
                        taskAdapter.notifyItemChanged(position);
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                );
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void openCreateTask() {
        Intent intent = new Intent(ProjectDetailActivity.this, CreateTaskActivity.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("projectName", projectName);
        startActivity(intent);
    }

    private void showAddMemberToProjectDialog() {
        progressBar.setVisibility(View.VISIBLE);

        // Lấy tất cả users trong hệ thống
        userRepo.getAllUsers(
            allUsers -> {
                progressBar.setVisibility(View.GONE);

                if (allUsers.isEmpty()) {
                    Toast.makeText(this, "Không có user nào trong hệ thống", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Filter out users đã là members
                List<User> availableUsers = new ArrayList<>();
                for (User user : allUsers) {
                    boolean isMember = false;
                    for (User member : projectMembers) {
                        if (user.getUserId().equals(member.getUserId())) {
                            isMember = true;
                            break;
                        }
                    }
                    if (!isMember) {
                        availableUsers.add(user);
                    }
                }

                if (availableUsers.isEmpty()) {
                    Toast.makeText(this, "Tất cả users đã là members của project", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Hiển thị dialog chọn users
                showUserSelectionDialog(availableUsers);
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải danh sách users: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showUserSelectionDialog(List<User> availableUsers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_members, null);
        builder.setView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewMembers);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> emptyList = new ArrayList<>(); // Không có ai được chọn trước
        MemberSelectAdapter adapter = new MemberSelectAdapter(this, availableUsers, emptyList);
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            List<String> selectedUserIds = adapter.getSelectedMemberIds();

            if (selectedUserIds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 user", Toast.LENGTH_SHORT).show();
                return;
            }

            addMembersToProject(selectedUserIds, availableUsers);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addMembersToProject(List<String> userIds, List<User> availableUsers) {
        progressBar.setVisibility(View.VISIBLE);

        final int[] addedCount = {0};
        final int totalToAdd = userIds.size();

        for (String userId : userIds) {
            // Tìm user info
            User user = null;
            for (User u : availableUsers) {
                if (u.getUserId().equals(userId)) {
                    user = u;
                    break;
                }
            }

            if (user == null) continue;

            // Tạo ProjectMember
            com.fptu.prm392.mad.models.ProjectMember newMember =
                new com.fptu.prm392.mad.models.ProjectMember(
                    user.getUserId(),
                    user.getFullname(),
                    user.getEmail(),
                    user.getAvatar(),
                    "member" // role
                );

            // Thêm vào project
            projectRepo.addMember(projectId, newMember,
                aVoid -> {
                    addedCount[0]++;
                    if (addedCount[0] == totalToAdd) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Đã thêm " + totalToAdd + " members vào project!",
                            Toast.LENGTH_SHORT).show();

                        // Reload project info and members
                        loadProjectInfo();
                        loadProjectMembers();
                    }
                },
                error -> {
                    addedCount[0]++;
                    if (addedCount[0] == totalToAdd) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Đã thêm members (có lỗi xảy ra)",
                            Toast.LENGTH_SHORT).show();
                        loadProjectInfo();
                        loadProjectMembers();
                    }
                }
            );
        }
    }

    private void showChangeStatusDialog(Task task, int position) {
        String[] statusOptions = {"TODO", "ĐANG LÀM", "HOÀN THÀNH"};
        String[] statusValues = {"todo", "in_progress", "done"};

        // Tìm index hiện tại
        int currentIndex = 0;
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(task.getStatus())) {
                currentIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thay đổi trạng thái")
            .setSingleChoiceItems(statusOptions, currentIndex, (dialog, which) -> {
                // Update status
                String newStatus = statusValues[which];
                task.setStatus(newStatus);

                progressBar.setVisibility(View.VISIBLE);

                taskRepo.updateTask(task,
                    aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();

                        // Cập nhật UI
                        taskList.set(position, task);
                        taskAdapter.notifyItemChanged(position);

                        dialog.dismiss();
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                );
            })
            .setNegativeButton("Hủy", null);

        builder.create().show();
    }
}
