package com.fptu.prm392.mad;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.MemberSelectableAdapter;
import com.fptu.prm392.mad.adapters.SelectedAssigneeAdapter;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateTaskActivity extends AppCompatActivity {

    private ImageView btnBack, btnAddAssignees;
    private TextInputEditText etTaskTitle, etTaskDescription;
    private LinearLayout layoutDueDate;
    private TextView tvDueDate;
    private RadioGroup rgStatus;
    private RadioButton rbTodo, rbInProgress, rbDone;
    private Button btnCreateTask;
    private RecyclerView rvSelectedAssignees;

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private String projectId;
    private Date selectedDueDate = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private SelectedAssigneeAdapter selectedAssigneeAdapter;
    private List<ProjectMember> projectMembers = new ArrayList<>();
    private List<ProjectMember> selectedAssignees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        taskRepository = new TaskRepository();
        projectRepository = new ProjectRepository();

        // Get projectId from intent
        projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null) {
            Toast.makeText(this, "Error: Project not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        layoutDueDate = findViewById(R.id.layoutDueDate);
        tvDueDate = findViewById(R.id.tvDueDate);
        rgStatus = findViewById(R.id.rgStatus);
        rbTodo = findViewById(R.id.rbTodo);
        rbInProgress = findViewById(R.id.rbInProgress);
        rbDone = findViewById(R.id.rbDone);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnAddAssignees = findViewById(R.id.btnAddAssignees);
        rvSelectedAssignees = findViewById(R.id.rvSelectedAssignees);

        // Setup RecyclerView for selected assignees
        rvSelectedAssignees.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedAssigneeAdapter = new SelectedAssigneeAdapter((member, position) -> {
            selectedAssignees.remove(position);
            selectedAssigneeAdapter.setSelectedMembers(selectedAssignees);
        });
        rvSelectedAssignees.setAdapter(selectedAssigneeAdapter);

        // Setup listeners
        btnBack.setOnClickListener(v -> finish());
        layoutDueDate.setOnClickListener(v -> showDatePicker());
        btnAddAssignees.setOnClickListener(v -> showSelectAssigneesDialog());
        btnCreateTask.setOnClickListener(v -> createTask());

        // Load project members
        loadProjectMembers();
    }

    private void loadProjectMembers() {
        projectRepository.getProjectMembers(projectId,
                members -> {
                    projectMembers = members;
                },
                e -> {
                    Toast.makeText(this, "Error loading members: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showSelectAssigneesDialog() {
        if (projectMembers.isEmpty()) {
            Toast.makeText(this, "Loading members...", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_assignees);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize views
        EditText etSearch = dialog.findViewById(R.id.etSearchAssignee);
        RecyclerView rvMembers = dialog.findViewById(R.id.rvMembers);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        // Setup RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        MemberSelectableAdapter adapter = new MemberSelectableAdapter();
        adapter.setMembers(projectMembers);
        adapter.setSelectedMembers(selectedAssignees);
        rvMembers.setAdapter(adapter);

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Buttons
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            selectedAssignees = adapter.getSelectedMembers();
            selectedAssigneeAdapter.setSelectedMembers(selectedAssignees);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // If a date is already selected, use it as the initial date
        if (selectedDueDate != null) {
            calendar.setTime(selectedDueDate);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Create selected date
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);
                    Date selected = selectedCalendar.getTime();

                    // Get current date (start of today)
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    // Check if selected date is in the past
                    if (selected.before(today.getTime())) {
                        Toast.makeText(this, "Due date cannot be in the past. Please select a valid date.",
                                Toast.LENGTH_LONG).show();
                        // Show date picker again
                        showDatePicker();
                    } else {
                        selectedDueDate = selected;
                        tvDueDate.setText(dateFormat.format(selectedDueDate));
                        tvDueDate.setTextColor(getResources().getColor(android.R.color.black, null));
                    }
                },
                year,
                month,
                day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void createTask() {
        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();

        // Validation
        if (title.isEmpty()) {
            etTaskTitle.setError("Task title is required");
            etTaskTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etTaskDescription.setError("Task description is required");
            etTaskDescription.requestFocus();
            return;
        }

        // Get selected status
        String status = "todo"; // Default
        int selectedId = rgStatus.getCheckedRadioButtonId();
        if (selectedId == R.id.rbTodo) {
            status = "todo";
        } else if (selectedId == R.id.rbInProgress) {
            status = "in_progress";
        } else if (selectedId == R.id.rbDone) {
            status = "done";
        }

        // Convert due date to Timestamp (nullable)
        Timestamp dueTimestamp = null;
        if (selectedDueDate != null) {
            dueTimestamp = new Timestamp(selectedDueDate);
        }

        // Disable button to prevent double-click
        btnCreateTask.setEnabled(false);

        // Prepare assignees list - creator is always the first assignee
        List<String> assigneeIds = new ArrayList<>();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        assigneeIds.add(currentUserId); // Người tạo luôn là assignee đầu tiên

        // Add selected assignees (avoid duplicates)
        for (ProjectMember member : selectedAssignees) {
            if (!assigneeIds.contains(member.getUserId())) {
                assigneeIds.add(member.getUserId());
            }
        }

        // Create task with status and due date
        final String finalStatus = status;
        final Timestamp finalDueTimestamp = dueTimestamp;

        taskRepository.createTask(projectId, title, description,
                taskId -> {
                    // Task created, now update status, dueDate, and assignees
                    taskRepository.updateTaskDetails(taskId, finalStatus, finalDueTimestamp, assigneeIds,
                            aVoid -> {
                                Toast.makeText(CreateTaskActivity.this,
                                        "Task created successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            e -> {
                                Toast.makeText(CreateTaskActivity.this,
                                        "Task created but failed to update details: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                btnCreateTask.setEnabled(true);
                            }
                    );
                },
                e -> {
                    Toast.makeText(CreateTaskActivity.this,
                            "Error creating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnCreateTask.setEnabled(true);
                }
        );
    }
}

