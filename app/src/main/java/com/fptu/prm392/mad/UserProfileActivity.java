package com.fptu.prm392.mad;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.repositories.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView btnBack, ivProfileAvatar;
    private TextView tvProfileFullname, tvProfileEmail, tvTaskCount, tvCreatedAt;

    private UserRepository userRepository;
    private com.fptu.prm392.mad.repositories.TaskRepository taskRepository;
    private String userId;
    private String projectId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userRepository = new UserRepository();
        taskRepository = new com.fptu.prm392.mad.repositories.TaskRepository();

        // Get userId and projectId from intent
        userId = getIntent().getStringExtra("USER_ID");
        projectId = getIntent().getStringExtra("PROJECT_ID");

        if (userId == null) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileFullname = findViewById(R.id.tvProfileFullname);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvTaskCount = findViewById(R.id.tvTaskCount);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);

        // Setup listeners
        btnBack.setOnClickListener(v -> finish());

        // Load user profile
        loadUserProfile();

        // Load task count if projectId is provided
        if (projectId != null) {
            loadTaskCount();
        }
    }

    private void loadUserProfile() {
        userRepository.getUserById(userId,
                user -> {
                    // Display user info
                    if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                        tvProfileFullname.setText(user.getFullname());
                    } else {
                        tvProfileFullname.setText("No name");
                    }

                    tvProfileEmail.setText(user.getEmail());

                    // Display created date if available
                    if (user.getCreatedAt() != null) {
                        tvCreatedAt.setText(dateFormat.format(user.getCreatedAt().toDate()));
                    } else {
                        tvCreatedAt.setText("N/A");
                    }
                },
                e -> {
                    Toast.makeText(this, "Error loading user profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
    }

    private void loadTaskCount() {
        // Query tasks where this user is assigned in this project
        taskRepository.getTasksByProject(projectId,
                tasks -> {
                    // Count tasks where user is in assignees list
                    int count = 0;
                    for (com.fptu.prm392.mad.models.Task task : tasks) {
                        if (task.getAssignees() != null && task.getAssignees().contains(userId)) {
                            count++;
                        }
                    }
                    tvTaskCount.setText(String.valueOf(count));
                },
                e -> {
                    // If error, just show 0
                    tvTaskCount.setText("0");
                }
        );
    }
}

