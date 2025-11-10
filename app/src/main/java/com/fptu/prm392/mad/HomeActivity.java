package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.Domains.Projects.Activities.ProjectDetailActivity;
import com.fptu.prm392.mad.Domains.Projects.Models.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private Button btnLogout, btnCreateProject, btnViewProjectDetail, btnViewTask;
    private EditText etProjectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ view
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        btnViewProjectDetail = findViewById(R.id.btnViewProjectDetail);
        btnViewTask = findViewById(R.id.btnViewTask);
        etProjectName = findViewById(R.id.etProjectName);

        // Lấy thông tin user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            tvWelcome.setText("Chào mừng, " + email);
        } else {
            navigateToLogin();
        }

        // ====== Sự kiện ======

        // Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(HomeActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });

        // Tạo project mới
        btnCreateProject.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CreateProjectActivity.class);
            startActivity(intent);
        });

        // Xem chi tiết project
        btnViewProjectDetail.setOnClickListener(v -> {
            String queryText = etProjectName.getText().toString().trim();
            if (queryText.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên hoặc ID project", Toast.LENGTH_SHORT).show();
            } else {
                searchProject(queryText);
            }
        });

        // Nút View Task (bạn có thể mở màn khác nếu cần)
        btnViewTask.setOnClickListener(v -> {
            String queryText = etProjectName.getText().toString().trim();
            if (queryText.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên hoặc ID project", Toast.LENGTH_SHORT).show();
            } else {
                openTaskListByQuery(queryText);
            }
        });
    }

    // ================= FIRESTORE SEARCH =================

    private void searchProject(String queryText) {
        // 1️⃣ Thử tìm theo Document ID
        db.collection("projects").document(queryText).get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        Project project = document.toObject(Project.class);
                        if (project != null) {
                            navigateToProjectDetail(project);
                        }
                    } else {
                        // Không có, thử tìm theo tên
                      searchProjectByName(queryText);
                    }
                })
                .addOnFailureListener(e -> {
                    searchProjectByName(queryText);
                });
    }

    private void searchProjectByName(String name) {
        db.collection("projects")
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Project project = document.toObject(Project.class);
                        if (project != null) {
                            navigateToProjectDetail(project);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy project với ID hoặc tên này", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tìm kiếm: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ================= CHUYỂN MÀN =================

    private void navigateToProjectDetail(Project project) {
        Intent intent = new Intent(HomeActivity.this, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_DETAIL", project);
        startActivity(intent);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, com.fptu.prm392.mad.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openTaskListByQuery(String queryText) {
        db.collection("projects").document(queryText).get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        // Nếu queryText là Document ID thật
                        String projectId = document.getId();
                        openTaskListActivity(projectId);
                    } else {
                        // Nếu không phải ID thì thử tìm theo tên
                        db.collection("projects")
                                .whereEqualTo("name", queryText)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                        String projectId = doc.getId();
                                        openTaskListActivity(projectId);
                                    } else {
                                        Toast.makeText(this, "Không tìm thấy project", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tìm project: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void openTaskListActivity(String projectId) {
        Intent intent = new Intent(HomeActivity.this, TaskListActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

}
