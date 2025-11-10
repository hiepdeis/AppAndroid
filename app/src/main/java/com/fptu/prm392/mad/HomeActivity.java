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
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.fptu.prm392.mad.fragments.ChatDetailFragment;
import com.fptu.prm392.mad.fragments.ChatListFragment;
import com.fptu.prm392.mad.fragments.ProjectListFragment;
import com.fptu.prm392.mad.fragments.TaskListFragment;

import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide; // Import Glide
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.Map;

public class HomeActivity extends AppCompatActivity {

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

    private ImageView ivProfileAvatar; // Khai báo ở đây để dễ truy cập

    // ActivityResultLauncher để xử lý kết quả chọn ảnh
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
                  //  showOtherTab("Calendar");
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

        // Xử lý tạo dự án mới
        fabCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, CreateProjectActivity.class);
                startActivity(intent);
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // Hiển thị popup xác nhận
                            showUploadConfirmation(selectedImageUri);
                        }
                    }
                }
        );
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

    private void showCalendarTab() {
        projectFragmentContainer.setVisibility(View.GONE);
        taskFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);  // ẩn container text tạm
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
        contentArea.setBackgroundResource(R.drawable.img_3); // Hoặc màu/ảnh nền bạn muốn


    }

    private void showProjectsTab() {
        projectFragmentContainer.setVisibility(View.VISIBLE);
        taskFragmentContainer.setVisibility(View.GONE);
        chatFragmentContainer.setVisibility(View.GONE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.VISIBLE);
        contentArea.setBackgroundResource(R.drawable.img_3);

        // Load project list fragment
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

        // Load task list fragment
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
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar); // Gán biến toàn cục

        TextView tvProfileFullname = findViewById(R.id.tvProfileFullname);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        Button btnProfileSignOut = findViewById(R.id.btnProfileSignOut);
        // GÁN SỰ KIỆN CLICK CHO ẢNH ĐẠI DIỆN
        ivProfileAvatar.setOnClickListener(v -> openImagePicker());

        // Setup sign out button
        btnProfileSignOut.setOnClickListener(v -> showSignOutConfirmation());

        userRepository.getUserById(currentUserId,
            user -> {
                if (user == null) {
                    Toast.makeText(this, "Could not load user data.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Display user info
                if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                    tvProfileFullname.setText(user.getFullname());
                } else {
                    tvProfileFullname.setText("No name");
                }

                tvProfileEmail.setText(user.getEmail());
                // Hiển thị tên và email
                if (user.getFullname() != null && !user.getFullname().isEmpty()) {
                    tvProfileFullname.setText(user.getFullname());
                } else {
                    tvProfileFullname.setText("No name");
                }
                tvProfileEmail.setText(user.getEmail());

                // HIỂN THỊ ẢNH ĐẠI DIỆN BẰNG GLIDE
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    Glide.with(this)
                            .load(user.getAvatar())
                            .circleCrop()
                            .placeholder(R.drawable.add_people) // << Thay bằng ảnh mặc định của bạn
                            .error(R.drawable.add_people)       // << Thay bằng ảnh mặc định của bạn
                            .into(ivProfileAvatar);
                } else {
                    // Nếu không có URL, hiển thị ảnh mặc định
                    Glide.with(this)
                            .load(R.drawable.add_people) // << Thay bằng ảnh mặc định của bạn
                            .circleCrop()
                            .into(ivProfileAvatar);
                }
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
        chatFragmentContainer.setVisibility(View.VISIBLE);
        otherTabsContainer.setVisibility(View.GONE);
        profileContainer.setVisibility(View.GONE);
        fabCreateProject.setVisibility(View.GONE);
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
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showUploadConfirmation(Uri imageUri) {
        ImageView previewImage = new ImageView(this);
        previewImage.setImageURI(imageUri);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600
        );
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(margin, margin, margin, margin);
        previewImage.setLayoutParams(layoutParams);
        previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Upload")
                .setMessage("Do you want to set this image as your profile picture?")
                .setView(previewImage)
                .setPositiveButton("Accept", (dialog, which) -> uploadImageToCloudinary(imageUri))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        Toast.makeText(HomeActivity.this, "Uploading...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(imageUri)
                .unsigned("my_unsigned_preset") // << QUAN TRỌNG: Thay bằng upload preset của bạn
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Đã có Toast ở trên
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        if (imageUrl != null) {
                            Toast.makeText(HomeActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                            saveImageUrlToFirebase(imageUrl);
                        } else {
                            Toast.makeText(HomeActivity.this, "Upload failed: URL is null.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(HomeActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                }).dispatch();
    }

    private void saveImageUrlToFirebase(String imageUrl) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        userRepository.updateUserAvatar(currentUserId, imageUrl,
                aVoid -> {
                    // Cập nhật lại ảnh đại diện ngay lập tức
                    if (ivProfileAvatar != null) {
                        Glide.with(HomeActivity.this)
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.add_people)
                                .into(ivProfileAvatar);
                    }
                    Toast.makeText(this, "Profile picture updated.", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}
