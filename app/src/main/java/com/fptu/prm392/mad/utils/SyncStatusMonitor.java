package com.fptu.prm392.mad.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

/**
 * Lưu trữ tạm các tác vụ chờ sync (vd: project tạo khi offline) và kiểm tra khi online lại.
 */
public class SyncStatusMonitor {
    private static final String TAG = "SyncStatusMonitor";
    private static final String PREFS_NAME = "sync_status_prefs";
    private static final String KEY_PENDING_PROJECTS = "pending_projects"; // format: projectId|projectName

    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private SyncStatusListener listener;

    public interface SyncStatusListener {
        void onProjectSynced(String projectId, String projectName);
    }

    public SyncStatusMonitor(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
    }

    public void setSyncStatusListener(SyncStatusListener listener) {
        this.listener = listener;
    }

    public void addPendingProject(String projectId, String projectName) {
        Set<String> pending = getPendingProjects();
        pending.add(projectId + "|" + projectName);
        savePendingProjects(pending);
        Log.d(TAG, "Pending project queued: " + projectId);
    }

    public void checkAndSyncPendingProjects() {
        Set<String> pending = getPendingProjects();
        if (pending.isEmpty()) {
            return;
        }

        Log.d(TAG, "Checking " + pending.size() + " pending projects");

        for (String projectData : new HashSet<>(pending)) {
            String[] parts = projectData.split("\\|");
            if (parts.length != 2) continue;

            String projectId = parts[0];
            String projectName = parts[1];

            db.collection("projects")
                .document(projectId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        removePendingProject(projectData);
                        if (listener != null) {
                            listener.onProjectSynced(projectId, projectName);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking project sync", e));
        }
    }

    private Set<String> getPendingProjects() {
        return new HashSet<>(prefs.getStringSet(KEY_PENDING_PROJECTS, new HashSet<>()));
    }

    private void savePendingProjects(Set<String> projects) {
        prefs.edit().putStringSet(KEY_PENDING_PROJECTS, projects).apply();
    }

    private void removePendingProject(String projectData) {
        Set<String> pending = getPendingProjects();
        if (pending.remove(projectData)) {
            savePendingProjects(pending);
        }
    }
}
