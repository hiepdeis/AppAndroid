package com.fptu.prm392.mad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.fptu.prm392.mad.fragments.ChatDetailFragment;
import com.fptu.prm392.mad.fragments.ChatListFragment;
import com.fptu.prm392.mad.fragments.ProjectListFragment;
import com.fptu.prm392.mad.fragments.TaskListFragment;
import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.fptu.prm392.mad.utils.NetworkMonitor;
import com.fptu.prm392.mad.utils.NotificationHelper;
import com.fptu.prm392.mad.utils.SyncStatusMonitor;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    private FirebaseAuth mAuth;
    private Button btnLogout;
    private ImageView fabCreateProject;
    private BottomNavigationView bottomNavigationView;

    // Containers
    private FrameLayout contentArea;
    private FrameLayout projectFragmentContainer, taskFragmentContainer, chatFragmentContainer;
    private LinearLayout otherTabsContainer;
    private ScrollView profileContainer;

    // Fragments
    private ProjectListFragment projectListFragment;
    private TaskListFragment taskListFragment;
    private ChatListFragment chatListFragment;
    private ChatDetailFragment chatDetailFragment;

    // Repositories
    private UserRepository userRepository;

    private TextView tvTabMessage;
    private LinearLayout networkStatusBanner;
    private TextView tvNetworkStatus, tvNetworkStatusDesc;
    private NetworkMonitor networkMonitor;
    private SyncStatusMonitor syncStatusMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Initialize views
        btnLogout = findViewById(R.id.btnLogout);
        fabCreateProject = findViewById(R.id.fabCreateProject);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        contentArea = findViewById(R.id.contentArea);
        projectFragmentContainer = findViewById(R.id.projectFragmentContainer);
        taskFragmentContainer = findViewById(R.id.taskFragmentContainer);
        chatFragmentContainer = findViewById(R.id.chatFragmentContainer);
        otherTabsContainer = findViewById(R.id.otherTabsContainer);
        profileContainer = findViewById(R.id.profileContainer);
        tvTabMessage = findViewById(R.id.tvTabMessage);
        networkStatusBanner = findViewById(R.id.networkStatusBanner);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvNetworkStatusDesc = findViewById(R.id.tvNetworkStatusDesc);

        // Setup monitoring utilities
        setupNetworkMonitor();
        setupSyncStatusMonitor();
        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermission();

        // Force disable icon tint để hiển thị màu gốc của PNG
        bottomNavigationView.setItemIconTintList(null);

        // Lấy thông tin user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        // Initialize fragments
        projectListFragment = ProjectListFragment.newInstance();
        projectListFragment.setOnProjectClickListener(this::openProjectDetail);

        taskListFragment = TaskListFragment.newInstance();
        taskListFragment.setOnTaskClickListener(this::openTaskDetail);

        chatListFragment = ChatListFragment.newInstance();
        chatListFragment.setOnChatClickListener(this::openChatDetail);

        // Xử lý back button
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Check if opened from Project/Task chat button
        handleIncomingIntent();

        // Tự động chọn tab Project khi vào màn hình (nếu không có intent đặc biệt)
        if (!getIntent().hasExtra("OPEN_CHAT_ID")) {
            bottomNavigationView.setSelectedItemId(R.id.nav_project);
            showProjectsTab();
        }

        // Xử lý sự kiện click bottom navigation
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_project) {
                    showProjectsTab();
                    return true;
                } else if (itemId == R.id.nav_task) {
                    showTaskTab();
                    return true;
                } else if (itemId == R.id.nav_calendar) {
                    showOtherTab("Calendar");
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    showChatTab();
                    return true;
                } else if (itemId == R.id.nav_notification) {
                    showOtherTab("Notifications");
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    showProfileTab();
                    return true;
                }
                return false;
            }
        });

        // Đăng xuất
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        // Tạo project mới
        fabCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, CreateProjectActivity.class);
                startActivity(intent);
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("OPEN_CHAT_ID")) {
            String chatId = intent.getStringExtra("OPEN_CHAT_ID");
            String chatName = intent.getStringExtra("CHAT_NAME");

            bottomNavigationView.setSelectedItemId(R.id.nav_chat);
            showChatTab();

            if (chatId != null && chatName != null) {
                Chat chat = new Chat();
                chat.setChatId(chatId);
                chat.setProjectName(chatName);
                openChatDetail(chat);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profileContainer.getVisibility() == View.VISIBLE) {
            loadUserProfile();
        }
    }

    private void showProjectsTab() {
        projectFragmentContainer.setVisibility(View.VISIBLE);
        taskFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.VISIBLE);
        contentArea.setBackgroundResource(R.drawable.img_3);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.projectFragmentContainer, projectListFragment);
        transaction.commit();
    }

    private void showTaskTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.VISIBLE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_2);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.taskFragmentContainer, taskListFragment);
        transaction.commit();
    }

    private void showOtherTab(String tabName) {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.VISIBLE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_3);
        tvTabMessage.setText(tabName + " - Coming soon...");
    }

    private void showProfileTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.VISIBLE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.profile_backgrounf);
        loadUserProfile();
    }

    private void showChatTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.VISIBLE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.chat_background);

        showChatListFragment();
    }

    private void showChatListFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.chatFragmentContainer, chatListFragment);
        transaction.commit();
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    private void openProjectDetail(Project project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_ID", project.getProjectId());
        startActivity(intent);
    }

    private void openTaskDetail(Task task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("TASK_ID", task.getTaskId());
        startActivity(intent);
    }

    private void openChatDetail(Chat chat) {
        chatDetailFragment = ChatDetailFragment.newInstance(chat.getChatId(), chat.getProjectName());
        chatDetailFragment.setOnBackToChatsListener(this::showChatListFragment);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.chatFragmentContainer, chatDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        bottomNavigationView.setVisibility(View.GONE);
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        ImageView ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        TextView tvProfileFullname = findViewById(R.id.tvProfileFullname);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        Button btnProfileSignOut = findViewById(R.id.btnProfileSignOut);

        btnProfileSignOut.setOnClickListener(v -> showSignOutConfirmation());

        userRepository.getUserById(currentUserId,
            user -> {
                if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                    tvProfileFullname.setText(user.getFullname());
                } else {
                    tvProfileFullname.setText("No name");
                }
                tvProfileEmail.setText(user.getEmail());
                // TODO: hiển thị avatar nếu có
            },
            e -> Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                Toast.LENGTH_SHORT).show());
    }

    private void showSignOutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out", (dialog, which) -> signOut())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void signOut() {
        mAuth.signOut();
        navigateToLogin();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
    }

    private void setupNetworkMonitor() {
        networkMonitor = NetworkMonitor.getInstance(this);
        networkMonitor.setNetworkStatusListener(isConnected -> runOnUiThread(() -> {
            if (isConnected) {
                networkStatusBanner.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Đã kết nối internet", Toast.LENGTH_SHORT).show();

                if (syncStatusMonitor != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        syncStatusMonitor.checkAndSyncPendingProjects();
                    }, 2000);
                }
            } else {
                networkStatusBanner.setVisibility(View.VISIBLE);
                tvNetworkStatus.setText("⚠️ Offline Mode");
                tvNetworkStatusDesc.setText("Dữ liệu sẽ đồng bộ khi có internet");
                Toast.makeText(HomeActivity.this, "Không có kết nối internet", Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void setupSyncStatusMonitor() {
        syncStatusMonitor = new SyncStatusMonitor(this);
        syncStatusMonitor.setSyncStatusListener((projectId, projectName) -> runOnUiThread(() -> {
            String message = "Project '" + projectName + "' đã được đồng bộ!";
            Toast.makeText(HomeActivity.this, "✅ " + message, Toast.LENGTH_LONG).show();

            if (NotificationHelper.isNotificationPermissionGranted(HomeActivity.this)) {
                NotificationHelper.showNotification(
                    HomeActivity.this,
                    "Đồng bộ thành công",
                    message,
                    projectId
                );
            }
        }));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền thông báo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null) {
            networkMonitor.removeNetworkStatusListener();
            networkMonitor.unregister();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, com.fptu.prm392.mad.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
