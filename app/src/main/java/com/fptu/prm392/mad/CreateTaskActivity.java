package com.fptu.prm392.mad;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.MemberSelectAdapter;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etTaskTitle, etTaskDescription;
    private Spinner spinnerStatus;
    private Button btnSelectDueDate, btnCreateTask, btnCancel, btnSelectMembers;
    private TextView tvSelectedDate;
    private ProgressBar progressBar;
    private LinearLayout layoutSelectedMembers;

    private FirebaseAuth mAuth;
    private TaskRepository taskRepo;
    private ProjectRepository projectRepo;
    private UserRepository userRepo;

    private String projectId;
    private Date selectedDueDate = null;
    private List<String> selectedMemberIds = new ArrayList<>();
    private List<User> projectMembers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Lấy projectId từ Intent
        projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        if (projectId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy dự án", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title nếu có ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tạo Task - " + projectName);
        }

        // Khởi tạo
        mAuth = FirebaseAuth.getInstance();
        taskRepo = new TaskRepository();
        projectRepo = new ProjectRepository();
        userRepo = new UserRepository();

        // Ánh xạ views
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSelectDueDate = findViewById(R.id.btnSelectDueDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancel);
        btnSelectMembers = findViewById(R.id.btnSelectMembers);
        layoutSelectedMembers = findViewById(R.id.layoutSelectedMembers);
        progressBar = findViewById(R.id.progressBar);

        // Setup Status Spinner
        setupStatusSpinner();

        // Load project members
        loadProjectMembers();

        // Xử lý chọn ngày
        btnSelectDueDate.setOnClickListener(v -> showDatePicker());

        // Xử lý chọn members
        btnSelectMembers.setOnClickListener(v -> showMemberSelectionDialog());

        // Xử lý tạo task
        btnCreateTask.setOnClickListener(v -> createTask());

        // Xử lý hủy
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupStatusSpinner() {
        String[] statuses = {"TODO", "ĐANG LÀM", "HOÀN THÀNH"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void loadProjectMembers() {
        // Load members của project từ ProjectMember collection
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

    private void showMemberSelectionDialog() {
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
        MemberSelectAdapter adapter = new MemberSelectAdapter(this, projectMembers, selectedMemberIds);
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            selectedMemberIds.clear();
            selectedMemberIds.addAll(adapter.getSelectedMemberIds());
            updateSelectedMembersUI();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSelectedMembersUI() {
        layoutSelectedMembers.removeAllViews();

        if (selectedMemberIds.isEmpty()) {
            return;
        }

        for (String userId : selectedMemberIds) {
            // Tìm user trong projectMembers
            User user = null;
            for (User u : projectMembers) {
                if (u.getUserId().equals(userId)) {
                    user = u;
                    break;
                }
            }

            if (user != null) {
                View memberView = LayoutInflater.from(this).inflate(
                        R.layout.item_assignee, layoutSelectedMembers, false);

                TextView tvName = memberView.findViewById(R.id.tvAssigneeName);
                TextView tvEmail = memberView.findViewById(R.id.tvAssigneeEmail);
                ImageButton btnRemove = memberView.findViewById(R.id.btnRemoveAssignee);

                tvName.setText(user.getFullname());
                tvEmail.setText(user.getEmail());

                User finalUser = user;
                btnRemove.setOnClickListener(v -> {
                    selectedMemberIds.remove(finalUser.getUserId());
                    updateSelectedMembersUI();
                });

                layoutSelectedMembers.addView(memberView);
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    selectedDueDate = selectedCalendar.getTime();

                    // Hiển thị ngày đã chọn
                    String dateStr = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    tvSelectedDate.setText("Hạn: " + dateStr);
                    tvSelectedDate.setVisibility(View.VISIBLE);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void createTask() {
        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();
        int statusPosition = spinnerStatus.getSelectedItemPosition();

        // Validation
        if (TextUtils.isEmpty(title)) {
            etTaskTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etTaskDescription.setError("Vui lòng nhập mô tả");
            return;
        }

        // Hiển thị progress
        progressBar.setVisibility(View.VISIBLE);
        btnCreateTask.setEnabled(false);

        String userId = mAuth.getCurrentUser().getUid();

        // Tạo Task object
        Task newTask = new Task(null, projectId, title, description, userId);

        // Set status dựa trên spinner
        switch (statusPosition) {
            case 0:
                newTask.setStatus("todo");
                break;
            case 1:
                newTask.setStatus("in_progress");
                break;
            case 2:
                newTask.setStatus("done");
                break;
        }

        // Set assignees
        newTask.setAssignees(new ArrayList<>(selectedMemberIds));

        // Set due date nếu có
        if (selectedDueDate != null) {
            newTask.setDueDate(new Timestamp(selectedDueDate));
        }

        // Lưu vào Firestore
        taskRepo.createTask(newTask,
                taskId -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Tạo task thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại màn hình trước
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateTask.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
