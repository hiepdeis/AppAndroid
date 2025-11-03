package com.fptu.prm392.mad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fptu.prm392.mad.utils.NetworkMonitor;
import com.fptu.prm392.mad.utils.NotificationHelper;
import com.fptu.prm392.mad.utils.SyncStatusMonitor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvWelcome;
    private Button btnLogout, btnCreateProject;
    private View networkStatusBanner;
    private TextView tvNetworkStatus, tvNetworkStatusDesc;
    private NetworkMonitor networkMonitor;
    private SyncStatusMonitor syncStatusMonitor;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnCreateProject = findViewById(R.id.btnCreateProject);
        networkStatusBanner = findViewById(R.id.networkStatusBanner);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvNetworkStatusDesc = findViewById(R.id.tvNetworkStatusDesc);

        // Setup Network Monitor (phải setup trước)
        setupNetworkMonitor();
        
        // Setup Sync Status Monitor (sau khi networkMonitor đã được khởi tạo)
        setupSyncStatusMonitor();

        // Tạo notification channel (bắt buộc cho Android 8.0+)
        NotificationHelper.createNotificationChannel(this);

        // Request notification permission (Android 13+)
        requestNotificationPermission();

        // Lấy thông tin user hiện tại
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            tvWelcome.setText("Chào mừng, " + email);
        } else {
            // Không có user, quay về màn hình login
            navigateToLogin();
        }

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
        btnCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển sang màn hình tạo project
                Intent intent = new Intent(HomeActivity.this, CreateProjectActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Request notification permission cho Android 13+
     */
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần quyền thông báo để nhận cập nhật", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Setup Network Monitor để theo dõi trạng thái mạng
     */
    private void setupNetworkMonitor() {
        networkMonitor = NetworkMonitor.getInstance(this);
        networkMonitor.setNetworkStatusListener(new NetworkMonitor.NetworkStatusListener() {
            @Override
            public void onNetworkStatusChanged(boolean isConnected) {
                runOnUiThread(() -> {
                    if (isConnected) {
                        // Online: Ẩn banner
                        networkStatusBanner.setVisibility(View.GONE);
                        Toast.makeText(HomeActivity.this, "Đã kết nối internet", Toast.LENGTH_SHORT).show();
                        
                        // Kiểm tra và sync pending projects khi online lại
                        if (syncStatusMonitor != null) {
                            // Đợi 2 giây để Firestore có thời gian sync
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                syncStatusMonitor.checkAndSyncPendingProjects();
                                syncStatusMonitor.checkAndSyncPendingNotifications();
                            }, 2000);
                        }
                    } else {
                        // Offline: Hiển thị banner
                        networkStatusBanner.setVisibility(View.VISIBLE);
                        tvNetworkStatus.setText("⚠️ Offline Mode");
                        tvNetworkStatusDesc.setText("Dữ liệu sẽ được đồng bộ khi có internet");
                        Toast.makeText(HomeActivity.this, "Không có kết nối internet", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * Setup Sync Status Monitor để thông báo khi sync thành công
     */
    private void setupSyncStatusMonitor() {
        syncStatusMonitor = new SyncStatusMonitor(this);
        syncStatusMonitor.setSyncStatusListener(new SyncStatusMonitor.SyncStatusListener() {
            @Override
            public void onProjectSynced(String projectId, String projectName) {
                runOnUiThread(() -> {
                    // Hiển thị notification
                    NotificationHelper.showNotification(
                        HomeActivity.this,
                        "Đồng bộ thành công",
                        "Project '" + projectName + "' đã được đồng bộ lên server",
                        projectId
                    );
                    
                    // Hiển thị toast
                    Toast.makeText(HomeActivity.this,
                        "✅ Project '" + projectName + "' đã được đồng bộ thành công!",
                        Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onNotificationSynced(String notificationId) {
                // Notification đã được sync (không cần thông báo riêng)
                Log.d("SyncStatusMonitor", "Notification synced: " + notificationId);
            }
        });
        
        // Kiểm tra ngay khi Activity khởi động (nếu đã online)
        // Đợi một chút để networkMonitor được khởi tạo xong
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (networkMonitor != null && networkMonitor.isNetworkAvailable()) {
                syncStatusMonitor.checkAndSyncPendingProjects();
                syncStatusMonitor.checkAndSyncPendingNotifications();
            }
        }, 3000); // Đợi 3 giây để đảm bảo networkMonitor đã ready
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister network callback khi activity bị destroy
        if (networkMonitor != null) {
            networkMonitor.removeNetworkStatusListener();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, com.fptu.prm392.mad.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
