package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.fragment.app.FragmentTransaction;

import com.fptu.prm392.mad.fragments.ChatDetailFragment;
import com.fptu.prm392.mad.fragments.ChatListFragment;
import com.fptu.prm392.mad.fragments.CalendarFragment;
import com.fptu.prm392.mad.fragments.ProjectListFragment;
import com.fptu.prm392.mad.fragments.TaskListFragment;
import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogout;
    private BottomNavigationView bottomNavigationView;

    // Containers
    private FrameLayout contentArea;
    private FrameLayout projectFragmentContainer, taskFragmentContainer, chatFragmentContainer, calendarFragmentContainer;
    private LinearLayout otherTabsContainer;
    private ScrollView profileContainer;

    // Fragments
    private ProjectListFragment projectListFragment;
    private TaskListFragment taskListFragment;
    private CalendarFragment calendarFragment;
    private ChatListFragment chatListFragment;
    private ChatDetailFragment chatDetailFragment;

    // Repositories
    private UserRepository userRepository;

    private TextView tvTabMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Initialize views
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        contentArea = findViewById(R.id.contentArea);
        projectFragmentContainer = findViewById(R.id.projectFragmentContainer);
        taskFragmentContainer = findViewById(R.id.taskFragmentContainer);
        calendarFragmentContainer = findViewById(R.id.calendarFragmentContainer);
        chatFragmentContainer = findViewById(R.id.chatFragmentContainer);
        otherTabsContainer = findViewById(R.id.otherTabsContainer);
        profileContainer = findViewById(R.id.profileContainer);
        tvTabMessage = findViewById(R.id.tvTabMessage);


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

        calendarFragment = CalendarFragment.newInstance();
        calendarFragment.setOnTaskClickListener(this::openTaskDetail);

        chatListFragment = ChatListFragment.newInstance();
        chatListFragment.setOnChatClickListener(this::openChatDetail);

        // Xử lý back button
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Nếu đang ở chat detail (có fragment trong back stack), hiện lại bottom nav
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().popBackStack();
                } else {
                    // Cho phép back mặc định (thoát app)
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
                    showCalendarTab();
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

        // Xử lý đăng xuất
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(HomeActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("OPEN_CHAT_ID")) {
            String chatId = intent.getStringExtra("OPEN_CHAT_ID");
            String chatName = intent.getStringExtra("CHAT_NAME");

            // Switch to chat tab
            bottomNavigationView.setSelectedItemId(R.id.nav_chat);
            showChatTab();

            // Open chat detail with the specific chat
            if (chatId != null && chatName != null) {
                // Create temporary Chat object
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
        // Fragments handle their own refresh
        if (profileContainer.getVisibility() == View.VISIBLE) {
            loadUserProfile();
        }
    }



    private void showProjectsTab() {
        projectFragmentContainer.setVisibility(View.VISIBLE);
        taskFragmentContainer.setVisibility(View.GONE);
        calendarFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_3);

        // Load project list fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.projectFragmentContainer, projectListFragment);
        transaction.commit();
    }

    private void showTaskTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.VISIBLE);
        calendarFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_2);

        // Load task list fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.taskFragmentContainer, taskListFragment);
        transaction.commit();
    }

    private void showCalendarTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        calendarFragmentContainer.setVisibility(View.VISIBLE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_3);

        // Load calendar fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.calendarFragmentContainer, calendarFragment);
        transaction.commit();
    }

    private void showOtherTab(String tabName) {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        calendarFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.VISIBLE);
        profileContainer.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_3);
        tvTabMessage.setText(tabName + " - Coming soon...");
    }

    private void showProfileTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        calendarFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.VISIBLE);
        contentArea.setBackgroundResource(R.drawable.profile_backgrounf);
        loadUserProfile();
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

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();


        // Find profile views
        ImageView ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        TextView tvProfileFullname = findViewById(R.id.tvProfileFullname);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        Button btnProfileSignOut = findViewById(R.id.btnProfileSignOut);

        // Setup sign out button
        btnProfileSignOut.setOnClickListener(v -> showSignOutConfirmation());

        userRepository.getUserById(currentUserId,
            user -> {
                // Display user info
                if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                    tvProfileFullname.setText(user.getFullname());
                } else {
                    tvProfileFullname.setText("No name");
                }

                tvProfileEmail.setText(user.getEmail());
            },
            e -> {
                Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showSignOutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out", (dialog, which) -> {
                signOut();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void signOut() {
        // Sign out from Firebase
        mAuth.signOut();

        // Redirect to login screen
        navigateToLogin();

        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
    }

    private void showChatTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        calendarFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.VISIBLE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.chat_background);

        // Show chat list fragment
        showChatListFragment();
    }

    private void showChatListFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.chatFragmentContainer, chatListFragment);
        transaction.commit();

        // Hiện lại bottom nav bar
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    private void openChatDetail(Chat chat) {
        // Create and show chat detail fragment
        chatDetailFragment = ChatDetailFragment.newInstance(chat.getChatId(), chat.getProjectName());
        chatDetailFragment.setOnBackToChatsListener(this::showChatListFragment);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.chatFragmentContainer, chatDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        // Ẩn bottom nav bar khi vào chat detail
        bottomNavigationView.setVisibility(View.GONE);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, com.fptu.prm392.mad.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
