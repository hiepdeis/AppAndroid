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
        String currentUserEmail = auth.getCurrentUser().getEmail();

        // Tạo document reference với ID tự động
        DocumentReference docRef = db.collection(COLLECTION_PROJECTS).document();
        String projectId = docRef.getId();

        // Tạo object Project
        Project project = new Project(projectId, name, description, currentUserId, currentUserName);

        // Lưu vào Firestore
        docRef.set(project)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Project created successfully: " + projectId);

                // Get current user's avatar from User collection, then create manager member
                UserRepository userRepository = new UserRepository();
                userRepository.getUserById(currentUserId,
                    user -> {
                        // Tạo manager member trong subcollection với avatar
                        com.fptu.prm392.mad.models.ProjectMember managerMember = new com.fptu.prm392.mad.models.ProjectMember(
                            projectId, // projectId
                            currentUserId,
                            currentUserName != null ? currentUserName : currentUserEmail,
                            currentUserEmail,
                            user.getAvatar(), // avatar from User collection
                            "manager"
                        );

                        db.collection(COLLECTION_PROJECTS)
                            .document(projectId)
                            .collection("members")
                            .document(currentUserId)
                            .set(managerMember)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Manager member added to project: " + projectId);
                                onSuccess.onSuccess(projectId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding manager member", e);
                                // Still return success for project creation
                                onSuccess.onSuccess(projectId);
                            });
                    },
                    e -> {
                        // If failed to get user, create member without avatar
                        Log.e(TAG, "Error getting user avatar", e);
                        com.fptu.prm392.mad.models.ProjectMember managerMember = new com.fptu.prm392.mad.models.ProjectMember(
                            projectId,
                            currentUserId,
                            currentUserName != null ? currentUserName : currentUserEmail,
                            currentUserEmail,
                            null, // fallback to null
                            "manager"
                        );

                        db.collection(COLLECTION_PROJECTS)
                            .document(projectId)
                            .collection("members")
                            .document(currentUserId)
                            .set(managerMember)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Manager member added (no avatar)");
                                onSuccess.onSuccess(projectId);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Error adding manager member", e2);
                                onSuccess.onSuccess(projectId);
                            });
                    }
                );
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
                    try {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            Log.d(TAG, "Project found: " + project.getName());
                            onSuccess.onSuccess(project);
                        } else {
                            onFailure.onFailure(new Exception("Project data is null"));
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Error deserializing project " + projectId + ": " + e.getMessage());
                        onFailure.onFailure(new Exception("Invalid project data structure: " + e.getMessage()));
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
            // NOTE: Bỏ orderBy để tránh lỗi missing composite index
            // Sẽ sort trên client-side thay thế
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Project> projects = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    try {
                        Project project = doc.toObject(Project.class);
                        if (project != null) {
                            projects.add(project);
                        }
                    } catch (RuntimeException e) {
                        // Skip projects with invalid data structure
                        Log.w(TAG, "Skipping project " + doc.getId() + " due to deserialization error: " + e.getMessage());
                    }
                }

                // Sort by createdAt descending trên client-side
                projects.sort((p1, p2) -> {
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                });

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
            // NOTE: Bỏ orderBy để tránh lỗi missing index
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Project> projects = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    try {
                        Project project = doc.toObject(Project.class);
                        if (project != null) {
                            projects.add(project);
                        }
                    } catch (RuntimeException e) {
                        // Skip projects with invalid data structure
                        Log.w(TAG, "Skipping project " + doc.getId() + " due to deserialization error: " + e.getMessage());
                    }
                }

                // Sort by createdAt descending trên client-side
                projects.sort((p1, p2) -> {
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                });

                Log.d(TAG, "Found " + projects.size() + " projects created by user");
                onSuccess.onSuccess(projects);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting created projects", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy tất cả projects trong hệ thống (cho tìm kiếm)
    public void getAllProjects(OnSuccessListener<List<Project>> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Project> projects = new ArrayList<>();
                int skippedCount = 0;
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    try {
                        Project project = doc.toObject(Project.class);
                        if (project != null) {
                            projects.add(project);
                        }
                    } catch (RuntimeException e) {
                        // Skip projects with invalid data structure
                        skippedCount++;
                        Log.w(TAG, "Skipping project " + doc.getId() + " - Name: " + doc.getString("name") +
                                " due to error: " + e.getMessage());
                        // Log memberIds để debug
                        Object memberIds = doc.get("memberIds");
                        Log.w(TAG, "  memberIds type: " + (memberIds != null ? memberIds.getClass().getName() : "null"));
                        Log.w(TAG, "  memberIds value: " + memberIds);
                    }
                }

                if (skippedCount > 0) {
                    Log.w(TAG, "Skipped " + skippedCount + " projects with invalid data");
                }

                // Sort by createdAt descending
                projects.sort((p1, p2) -> {
                    if (p1.getCreatedAt() == null) return 1;
                    if (p2.getCreatedAt() == null) return -1;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                });

                Log.d(TAG, "Found " + projects.size() + " valid projects in system");
                onSuccess.onSuccess(projects);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting all projects", e);
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

    // ============ PROJECT MEMBERS MANAGEMENT ============

    // Lấy danh sách members của project
    public void getProjectMembers(String projectId,
                                 OnSuccessListener<List<com.fptu.prm392.mad.models.ProjectMember>> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection("members")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<com.fptu.prm392.mad.models.ProjectMember> members = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    com.fptu.prm392.mad.models.ProjectMember member = doc.toObject(com.fptu.prm392.mad.models.ProjectMember.class);
                    if (member != null) {
                        members.add(member);
                    }
                }
                Log.d(TAG, "Found " + members.size() + " members in project " + projectId);
                onSuccess.onSuccess(members);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting project members", e);
                onFailure.onFailure(e);
            });
    }

    // Thêm member vào project
    public void addMemberToProject(String projectId, com.fptu.prm392.mad.models.ProjectMember member,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection("members")
            .document(member.getUserId())
            .set(member)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Member added to project: " + member.getUserId());
                // Also update memberIds array and memberCount in project
                updateProjectMemberArrays(projectId, member.getUserId(), true,
                    v -> onSuccess.onSuccess(aVoid), onFailure);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding member", e);
                onFailure.onFailure(e);
            });
    }

    // Xóa member khỏi project
    public void removeMemberFromProject(String projectId, String userId,
                                       OnSuccessListener<Void> onSuccess,
                                       OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection("members")
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Member removed from project: " + userId);
                // Also update memberIds array and memberCount in project
                updateProjectMemberArrays(projectId, userId, false,
                    v -> onSuccess.onSuccess(aVoid), onFailure);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error removing member", e);
                onFailure.onFailure(e);
            });
    }

    // Update memberIds array and memberCount in project document
    private void updateProjectMemberArrays(String projectId, String userId, boolean isAdding,
                                          OnSuccessListener<Void> onSuccess,
                                          OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> memberIds = (List<String>) doc.get("memberIds");
                    if (memberIds == null) memberIds = new ArrayList<>();

                    if (isAdding) {
                        if (!memberIds.contains(userId)) {
                            memberIds.add(userId);
                        }
                    } else {
                        memberIds.remove(userId);
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("memberIds", memberIds);
                    updates.put("memberCount", memberIds.size());

                    db.collection(COLLECTION_PROJECTS)
                        .document(projectId)
                        .update(updates)
                        .addOnSuccessListener(onSuccess)
                        .addOnFailureListener(onFailure);
                } else {
                    onFailure.onFailure(new Exception("Project not found"));
                }
            })
            .addOnFailureListener(onFailure);
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

    // MIGRATION: Fix memberIds field from HashMap to List<String>
    // Call this once to fix corrupted data in Firestore
    public void fixAllProjectsMemberIds(OnSuccessListener<String> onSuccess,
                                       OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int totalProjects = querySnapshot.size();
                int fixedCount = 0;
                int errorCount = 0;

                for (DocumentSnapshot doc : querySnapshot) {
                    try {
                        Object memberIds = doc.get("memberIds");

                        // Check if memberIds needs fixing
                        if (memberIds instanceof Map) {
                            // Convert Map to List<String>
                            Map<?, ?> memberIdsMap = (Map<?, ?>) memberIds;
                            List<String> memberIdsList = new ArrayList<>();

                            // Extract user IDs from map
                            for (Object value : memberIdsMap.values()) {
                                if (value instanceof String) {
                                    memberIdsList.add((String) value);
                                }
                            }

                            // Update document
                            db.collection(COLLECTION_PROJECTS)
                                .document(doc.getId())
                                .update("memberIds", memberIdsList)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Fixed memberIds for project: " + doc.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fixing project " + doc.getId(), e);
                                });

                            fixedCount++;
                        }
                    } catch (Exception e) {
                        errorCount++;
                        Log.e(TAG, "Error processing project " + doc.getId(), e);
                    }
                }

                String result = "Processed " + totalProjects + " projects. Fixed: " + fixedCount + ", Errors: " + errorCount;
                Log.d(TAG, result);
                onSuccess.onSuccess(result);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error in migration", e);
                onFailure.onFailure(e);
            });
    }
}
