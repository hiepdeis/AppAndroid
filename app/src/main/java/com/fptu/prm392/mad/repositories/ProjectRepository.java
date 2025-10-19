package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Project;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRepository {
    private static final String TAG = "ProjectRepository";
    private static final String COLLECTION_PROJECTS = "projects";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ProjectRepository() {
        // Kết nối Firestore tự động khi khởi tạo
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // CREATE: Tạo project mới
    public void createProject(String name, String description,
                             OnSuccessListener<String> onSuccess,
                             OnFailureListener onFailure) {

        String currentUserId = auth.getCurrentUser().getUid();
        String currentUserName = auth.getCurrentUser().getDisplayName();

        // Tạo document reference với ID tự động
        DocumentReference docRef = db.collection(COLLECTION_PROJECTS).document();
        String projectId = docRef.getId();

        // Tạo object Project
        Project project = new Project(projectId, name, description, currentUserId, currentUserName);

        // Lưu vào Firestore
        docRef.set(project)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Project created successfully: " + projectId);
                onSuccess.onSuccess(projectId); // Trả về projectId vừa tạo
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating project", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy project theo ID
    public void getProjectById(String projectId,
                              OnSuccessListener<Project> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Project project = documentSnapshot.toObject(Project.class);
                    if (project != null) {
                        Log.d(TAG, "Project found: " + project.getName());
                        onSuccess.onSuccess(project);
                    } else {
                        onFailure.onFailure(new Exception("Project data is null"));
                    }
                } else {
                    Log.d(TAG, "Project not found: " + projectId);
                    onFailure.onFailure(new Exception("Project not found"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting project", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy tất cả projects của user hiện tại
    public void getMyProjects(OnSuccessListener<List<Project>> onSuccess,
                             OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_PROJECTS)
            .whereArrayContains("memberIds", currentUserId) // Query theo memberIds
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Project> projects = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Project project = doc.toObject(Project.class);
                    if (project != null) {
                        projects.add(project);
                    }
                }
                Log.d(TAG, "Found " + projects.size() + " projects for user");
                onSuccess.onSuccess(projects);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting projects", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy projects do user tạo
    public void getProjectsCreatedByMe(OnSuccessListener<List<Project>> onSuccess,
                                      OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_PROJECTS)
            .whereEqualTo("createdBy", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Project> projects = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Project project = doc.toObject(Project.class);
                    if (project != null) {
                        projects.add(project);
                    }
                }
                Log.d(TAG, "Found " + projects.size() + " projects created by user");
                onSuccess.onSuccess(projects);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting created projects", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Cập nhật thông tin project
    public void updateProject(String projectId, Map<String, Object> updates,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Project updated successfully: " + projectId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating project", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Cập nhật tên project
    public void updateProjectName(String projectId, String newName,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updateProject(projectId, updates, onSuccess, onFailure);
    }

    // UPDATE: Cập nhật mô tả project
    public void updateProjectDescription(String projectId, String newDescription,
                                        OnSuccessListener<Void> onSuccess,
                                        OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", newDescription);
        updateProject(projectId, updates, onSuccess, onFailure);
    }

    // UPDATE: Tăng số lượng members
    public void incrementMemberCount(String projectId,
                                    OnSuccessListener<Void> onSuccess,
                                    OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Long currentCount = doc.getLong("memberCount");
                    int newCount = (currentCount != null ? currentCount.intValue() : 0) + 1;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("memberCount", newCount);
                    updateProject(projectId, updates, onSuccess, onFailure);
                } else {
                    onFailure.onFailure(new Exception("Project not found"));
                }
            })
            .addOnFailureListener(onFailure);
    }

    // UPDATE: Tăng số lượng tasks
    public void incrementTaskCount(String projectId,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Long currentCount = doc.getLong("taskCount");
                    int newCount = (currentCount != null ? currentCount.intValue() : 0) + 1;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("taskCount", newCount);
                    updateProject(projectId, updates, onSuccess, onFailure);
                } else {
                    onFailure.onFailure(new Exception("Project not found"));
                }
            })
            .addOnFailureListener(onFailure);
    }

    // DELETE: Xóa project
    public void deleteProject(String projectId,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Project deleted successfully: " + projectId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting project", e);
                onFailure.onFailure(e);
            });
    }

    // REALTIME: Lắng nghe thay đổi của một project
    public void listenToProject(String projectId,
                               OnSuccessListener<Project> onDataChanged) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen failed for project: " + projectId, error);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Project project = snapshot.toObject(Project.class);
                    if (project != null) {
                        Log.d(TAG, "Project data changed: " + projectId);
                        onDataChanged.onSuccess(project);
                    }
                }
            });
    }
}

