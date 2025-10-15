package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fptu.prm392.mad.auth.LoginActivity;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private UserRepository userRepo;

    private TextView tvWelcome, tvUserEmail, tvProjectCount;
    private Button btnLogout;
    private MaterialCardView cardCreateProject, cardViewProjects, cardMyProjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Khởi tạo Firebase Auth và Repository
        mAuth = FirebaseAuth.getInstance();
        userRepo = new UserRepository();

        // Ánh xạ các view
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvProjectCount = findViewById(R.id.tvProjectCount);
        btnLogout = findViewById(R.id.btnLogout);
        cardCreateProject = findViewById(R.id.cardCreateProject);
        cardViewProjects = findViewById(R.id.cardViewProjects);
        cardMyProjects = findViewById(R.id.cardMyProjects);

        // Lấy thông tin user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserInfo(currentUser.getUid());
        } else {
            // Nếu không có user, chuyển về màn hình đăng nhập
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Xử lý sự kiện đăng xuất
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // Xử lý sự kiện click Tạo Project
        cardCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến màn hình tạo project
                Intent intent = new Intent(HomeActivity.this, CreateProjectActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện click Danh Sách Projects
        cardViewProjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến màn hình danh sách project
                Intent intent = new Intent(HomeActivity.this, ProjectListActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện click Dự Án Của Tôi
        cardMyProjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến màn hình dự án của tôi
                Intent intent = new Intent(HomeActivity.this, MyProjectsActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadUserInfo(String userId) {
        userRepo.getUserById(userId,
            user -> {
                // Hiển thị thông tin user
                tvWelcome.setText("Chào " + user.getFullname() + "!");
                tvUserEmail.setText(user.getEmail());
            },
            error -> {
                Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                tvUserEmail.setText(mAuth.getCurrentUser().getEmail());
            }
        );
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();

        // Chuyển về màn hình đăng nhập
        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
        finish();
    }
}