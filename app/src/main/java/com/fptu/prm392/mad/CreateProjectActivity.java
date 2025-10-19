package com.fptu.prm392.mad;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.google.android.material.textfield.TextInputEditText;

public class CreateProjectActivity extends AppCompatActivity {

    private TextInputEditText etProjectName, etProjectDescription;
    private Button btnCreateProject;
    private ProgressBar progressBar;
    private ProjectRepository projectRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        // Khởi tạo Repository - Firestore tự động kết nối ở đây
        projectRepository = new ProjectRepository();

        // Ánh xạ các view
        etProjectName = findViewById(R.id.etProjectName);
        etProjectDescription = findViewById(R.id.etProjectDescription);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        progressBar = findViewById(R.id.progressBar);

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

        // GỌI FIRESTORE ĐỂ LƯU DỮ LIỆU
        projectRepository.createProject(
            projectName,
            projectDescription,

            // Callback khi thành công
            projectId -> {
                progressBar.setVisibility(View.GONE);
                btnCreateProject.setEnabled(true);

                Toast.makeText(CreateProjectActivity.this,
                    "✅ Tạo project thành công!\nID: " + projectId,
                    Toast.LENGTH_LONG).show();

                // Xóa dữ liệu input
                etProjectName.setText("");
                etProjectDescription.setText("");

                // Có thể chuyển sang màn hình khác hoặc đóng Activity
                // finish();
            },

            // Callback khi thất bại
            e -> {
                progressBar.setVisibility(View.GONE);
                btnCreateProject.setEnabled(true);

                Toast.makeText(CreateProjectActivity.this,
                    "❌ Lỗi: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        );
    }
}

