package com.fptu.prm392.mad;

import android.content.Intent;
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

public class MyProjectsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_my_projects);

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
        projectAdapter = new ProjectAdapter(this, projectList, project -> {
            // Mở chi tiết project
            Intent intent = new Intent(MyProjectsActivity.this, ProjectDetailActivity.class);
            intent.putExtra("projectId", project.getProjectId());
            intent.putExtra("projectName", project.getName());
            startActivity(intent);
        });
        recyclerViewProjects.setAdapter(projectAdapter);

        // Load projects
        loadMyProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload projects khi quay lại màn hình này
        loadMyProjects();
    }

    private void loadMyProjects() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        viewEmptyState.setVisibility(View.GONE);

        String userId = mAuth.getCurrentUser().getUid();

        // Lấy projects của user hiện tại
        projectRepo.getProjectsByUser(userId,
            projects -> {
                progressBar.setVisibility(View.GONE);

                if (projects.isEmpty()) {
                    // User chưa tham gia project nào
                    viewEmptyState.setVisibility(View.VISIBLE);
                    recyclerViewProjects.setVisibility(View.GONE);
                } else {
                    // Có projects
                    viewEmptyState.setVisibility(View.GONE);
                    recyclerViewProjects.setVisibility(View.VISIBLE);

                    projectList.clear();
                    projectList.addAll(projects);
                    projectAdapter.notifyDataSetChanged();

                    Toast.makeText(this, "Bạn tham gia " + projects.size() + " dự án",
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
