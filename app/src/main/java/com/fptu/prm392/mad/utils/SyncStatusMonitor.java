package com.fptu.prm392.mad.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class để monitor và thông báo khi sync operations hoàn thành
 */
public class SyncStatusMonitor {
    private static final String TAG = "SyncStatusMonitor";
    private static final String PREFS_NAME = "sync_status_prefs";
    private static final String KEY_PENDING_PROJECTS = "pending_projects";
    private static final String KEY_PENDING_NOTIFICATIONS = "pending_notifications";

    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private SyncStatusListener listener;

    public interface SyncStatusListener {
        void onProjectSynced(String projectId, String projectName);
        void onNotificationSynced(String notificationId);
    }

    public SyncStatusMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Thêm project ID vào danh sách pending (được tạo khi offline)
     */
    public void addPendingProject(String projectId, String projectName) {
        Set<String> pendingProjects = getPendingProjects();
        pendingProjects.add(projectId + "|" + projectName); // Format: "projectId|projectName"
        savePendingProjects(pendingProjects);
        Log.d(TAG, "Added pending project: " + projectId);
    }

    /**
     * Thêm notification ID vào danh sách pending
     */
    public void addPendingNotification(String notificationId) {
        Set<String> pendingNotifications = getPendingNotifications();
        pendingNotifications.add(notificationId);
        savePendingNotifications(pendingNotifications);
        Log.d(TAG, "Added pending notification: " + notificationId);
    }

    /**
     * Kiểm tra và sync các pending projects khi online
     */
    public void checkAndSyncPendingProjects() {
        Set<String> pendingProjects = getPendingProjects();
        if (pendingProjects.isEmpty()) {
            Log.d(TAG, "No pending projects to sync");
            return;
        }

        Log.d(TAG, "Checking " + pendingProjects.size() + " pending projects for sync");

        Set<String> syncedProjects = new HashSet<>();

        for (String projectData : pendingProjects) {
            String[] parts = projectData.split("\\|");
            if (parts.length != 2) continue;

            String projectId = parts[0];
            String projectName = parts[1];

            // Kiểm tra project có tồn tại trong Firestore không
            db.collection("projects")
                .document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Project đã được sync thành công
                        syncedProjects.add(projectData);
                        Log.d(TAG, "Project synced successfully: " + projectId);

                        // Thông báo cho listener
                        if (listener != null) {
                            listener.onProjectSynced(projectId, projectName);
                        }

                        // Xóa khỏi pending list
                        Set<String> updated = getPendingProjects();
                        updated.remove(projectData);
                        savePendingProjects(updated);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking project: " + projectId, e);
                });
        }
    }

    /**
     * Kiểm tra và sync các pending notifications
     */
    public void checkAndSyncPendingNotifications() {
        Set<String> pendingNotifications = getPendingNotifications();
        if (pendingNotifications.isEmpty()) {
            return;
        }

        for (String notificationId : pendingNotifications) {
            db.collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Notification đã được sync
                        if (listener != null) {
                            listener.onNotificationSynced(notificationId);
                        }

                        Set<String> updated = getPendingNotifications();
                        updated.remove(notificationId);
                        savePendingNotifications(updated);
                    }
                });
        }
    }

    /**
     * Đăng ký listener để nhận thông báo khi sync thành công
     */
    public void setSyncStatusListener(SyncStatusListener listener) {
        this.listener = listener;
    }

    /**
     * Xóa tất cả pending projects (sau khi đã sync xong)
     */
    public void clearPendingProjects() {
        savePendingProjects(new HashSet<>());
    }

    /**
     * Lấy số lượng pending projects
     */
    public int getPendingProjectsCount() {
        return getPendingProjects().size();
    }

    private Set<String> getPendingProjects() {
        return prefs.getStringSet(KEY_PENDING_PROJECTS, new HashSet<>());
    }

    private void savePendingProjects(Set<String> projects) {
        prefs.edit().putStringSet(KEY_PENDING_PROJECTS, projects).apply();
    }

    private Set<String> getPendingNotifications() {
        return prefs.getStringSet(KEY_PENDING_NOTIFICATIONS, new HashSet<>());
    }

    private void savePendingNotifications(Set<String> notifications) {
        prefs.edit().putStringSet(KEY_PENDING_NOTIFICATIONS, notifications).apply();
    }
}

