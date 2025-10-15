package com.fptu.prm392.mad;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.ProjectAdapter;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ProjectListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private ProgressBar progressBar;
    private View viewEmptyState;

    private FirebaseAuth mAuth;
    private ProjectRepository projectRepo;
    private List<Project> projectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        // Khởi tạo
        mAuth = FirebaseAuth.getInstance();
        projectRepo = new ProjectRepository();
        projectList = new ArrayList<>();

        // Ánh xạ views
        recyclerViewProjects = findViewById(R.id.recyclerViewProjects);
        progressBar = findViewById(R.id.progressBar);
        viewEmptyState = findViewById(R.id.tvEmptyState);

        // Setup RecyclerView
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter = new ProjectAdapter(this, projectList);
        recyclerViewProjects.setAdapter(projectAdapter);

        // Load projects
        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload projects khi quay lại màn hình này
        loadProjects();
    }

    private void loadProjects() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        viewEmptyState.setVisibility(View.GONE);

        // Lấy TẤT CẢ projects trong database (không phân biệt user)
        projectRepo.getAllProjects(
            projects -> {
                progressBar.setVisibility(View.GONE);

                if (projects.isEmpty()) {
                    // Không có project nào trong database
                    viewEmptyState.setVisibility(View.VISIBLE);
                    recyclerViewProjects.setVisibility(View.GONE);
                } else {
                    // Có projects
                    viewEmptyState.setVisibility(View.GONE);
                    recyclerViewProjects.setVisibility(View.VISIBLE);

                    projectList.clear();
                    projectList.addAll(projects);
                    projectAdapter.notifyDataSetChanged();

                    Toast.makeText(this, "Tìm thấy " + projects.size() + " dự án",
                        Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                viewEmptyState.setVisibility(View.VISIBLE);
                recyclerViewProjects.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }
}
