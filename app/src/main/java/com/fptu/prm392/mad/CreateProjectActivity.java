package com.fptu.prm392.mad;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.repositories.NotificationRepository;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.utils.NetworkMonitor;
import com.fptu.prm392.mad.utils.NotificationHelper;
import com.fptu.prm392.mad.utils.SyncStatusMonitor;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class CreateProjectActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText etProjectName, etProjectDescription;
    private Button btnCreateProject;
    private ProgressBar progressBar;
    private ProjectRepository projectRepository;
    private NotificationRepository notificationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        // Khởi tạo Repository - Firestore tự động kết nối ở đây
        projectRepository = new ProjectRepository();
        notificationRepository = new NotificationRepository();

        // Ánh xạ các view
        btnBack = findViewById(R.id.btnBack);
        etProjectName = findViewById(R.id.etProjectName);
        etProjectDescription = findViewById(R.id.etProjectDescription);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        progressBar = findViewById(R.id.progressBar);

        // Xử lý nút back
        btnBack.setOnClickListener(v -> finish());

        // Xử lý sự kiện khi bấm nút "Tạo Project"
        btnCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createProject();
            }
        });
    }

    private void createProject() {
        // Lấy dữ liệu từ input
        String projectName = etProjectName.getText().toString().trim();
        String projectDescription = etProjectDescription.getText().toString().trim();

        // Validate dữ liệu
        if (TextUtils.isEmpty(projectName)) {
            etProjectName.setError("Vui lòng nhập tên project");
            etProjectName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(projectDescription)) {
            etProjectDescription.setError("Vui lòng nhập mô tả project");
            etProjectDescription.requestFocus();
            return;
        }

        // Hiển thị loading
        progressBar.setVisibility(View.VISIBLE);
        btnCreateProject.setEnabled(false);

        // Kiểm tra trạng thái mạng hiện tại
        NetworkMonitor networkMonitor = NetworkMonitor.getInstance(this);
        boolean isOffline = !networkMonitor.isNetworkAvailable();
        if (isOffline) {
            Toast.makeText(this,
                "Không có kết nối internet. Yêu cầu sẽ được thực thi khi có mạng trở lại.",
                Toast.LENGTH_LONG).show();
            // Không giữ spinner nếu offline để tránh người dùng chờ vô hạn
            progressBar.setVisibility(View.GONE);
            btnCreateProject.setEnabled(true);
        }

        // GỌI FIRESTORE ĐỂ LƯU DỮ LIỆU
        projectRepository.createProject(
            projectName,
            projectDescription,

            // Callback khi thành công
            projectId -> {
                progressBar.setVisibility(View.GONE);
                btnCreateProject.setEnabled(true);

                if (isOffline) {
                    Toast.makeText(CreateProjectActivity.this,
                        "Project đã được lưu offline và sẽ đồng bộ khi có internet.",
                        Toast.LENGTH_LONG).show();

                    SyncStatusMonitor syncStatusMonitor = new SyncStatusMonitor(CreateProjectActivity.this);
                    syncStatusMonitor.addPendingProject(projectId, projectName);
                } else {
                    Toast.makeText(CreateProjectActivity.this,
                        "Project created successfully!",
                        Toast.LENGTH_SHORT).show();
                }

                // Save notification to Firestore for the creator
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                notificationRepository.saveNotificationToFirestore(
                    currentUserId,
                    "project_created",
                    "Project mới được tạo",
                    "Bạn đã tạo project: " + projectName,
                    projectId,
                    null,
                    notificationId -> {},
                    e -> {}
                );

                // Show local notification
                NotificationHelper.createNotificationChannel(this);
                if (NotificationHelper.isNotificationPermissionGranted(this)) {
                    NotificationHelper.showNotification(this,
                        "Project mới",
                        "Project '" + projectName + "' đã được tạo thành công",
                        projectId);
                }

                // Chuyển đến ProjectDetailActivity
                android.content.Intent intent = new android.content.Intent(
                    CreateProjectActivity.this,
                    ProjectDetailActivity.class
                );
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);

                // Đóng CreateProjectActivity
                finish();
            },

            // Callback khi thất bại
            e -> {
                progressBar.setVisibility(View.GONE);
                btnCreateProject.setEnabled(true);

                Toast.makeText(CreateProjectActivity.this,
                    " Lỗi: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        );
    }
}

