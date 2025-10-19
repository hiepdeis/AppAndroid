package com.fptu.prm392.mad;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CreateProjectActivity extends AppCompatActivity {

    private TextInputEditText etProjectName, etProjectDescription;
    private Button btnCreateProject, btnCancel;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private ProjectRepository projectRepo;
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_project);

        // Khởi tạo
        mAuth = FirebaseAuth.getInstance();
        projectRepo = new ProjectRepository();
        userRepo = new UserRepository();

        // Ánh xạ views
        etProjectName = findViewById(R.id.etProjectName);
        etProjectDescription = findViewById(R.id.etProjectDescription);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        // Xử lý nút Tạo Project
        btnCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createProject();
            }
        });

        // Xử lý nút Hủy
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void createProject() {
        String projectName = etProjectName.getText().toString().trim();
        String projectDescription = etProjectDescription.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(projectName)) {
            etProjectName.setError("Vui lòng nhập tên dự án");
            return;
        }

        if (TextUtils.isEmpty(projectDescription)) {
            etProjectDescription.setError("Vui lòng nhập mô tả dự án");
            return;
        }

        // Hiển thị progress
        progressBar.setVisibility(View.VISIBLE);
        btnCreateProject.setEnabled(false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // Lấy thông tin user để tạo ProjectMember
        userRepo.getUserById(userId,
            user -> {
                // Tạo project
                Project newProject = new Project(
                    null,
                    projectName,
                    projectDescription,
                    userId
                );

                // Tạo creator member với role = "owner"
                ProjectMember creator = new ProjectMember(
                    userId,
                    user.getFullname(),
                    user.getEmail(),
                    user.getAvatar(),
                    "owner"  // Creator = Owner
                );

                // Lưu vào Firestore
                projectRepo.createProject(newProject, creator,
                    projectId -> {
                        progressBar.setVisibility(View.GONE);
                        btnCreateProject.setEnabled(true);

                        Toast.makeText(CreateProjectActivity.this,
                            "Tạo dự án thành công!", Toast.LENGTH_SHORT).show();

                        // Quay về HomeActivity
                        finish();
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnCreateProject.setEnabled(true);

                        Toast.makeText(CreateProjectActivity.this,
                            "Lỗi: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                );
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                btnCreateProject.setEnabled(true);

                Toast.makeText(this, "Không thể lấy thông tin người dùng",
                    Toast.LENGTH_SHORT).show();
            }
        );
    }
}

