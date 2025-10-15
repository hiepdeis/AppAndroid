package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.ProjectMember;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRepository {
    private static final String TAG = "ProjectRepository";
    private static final String COLLECTION_PROJECTS = "projects";
    private static final String SUBCOLLECTION_MEMBERS = "members";

    private final FirebaseFirestore db;

    public ProjectRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // CREATE: Tạo project mới
    public void createProject(Project project, ProjectMember creator,
                             OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        // Tạo project document
        db.collection(COLLECTION_PROJECTS)
            .add(project)
            .addOnSuccessListener(documentReference -> {
                String projectId = documentReference.getId();
                project.setProjectId(projectId);

                // Thêm creator vào members
                db.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .collection(SUBCOLLECTION_MEMBERS)
                    .document(creator.getUserId())
                    .set(creator)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Project created: " + projectId);
                        onSuccess.onSuccess(projectId);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating project", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy project theo ID
    public void getProjectById(String projectId, OnSuccessListener<Project> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Project project = documentSnapshot.toObject(Project.class);
                    if (project != null) {
                        project.setProjectId(documentSnapshot.getId());
                        onSuccess.onSuccess(project);
                    } else {
                        onFailure.onFailure(new Exception("Project data is null"));
                    }
                } else {
                    onFailure.onFailure(new Exception("Project not found"));
                }
            })
            .addOnFailureListener(onFailure);
    }

    // READ: Lấy tất cả projects của user
    public void getProjectsByUser(String userId, OnSuccessListener<List<Project>> onSuccess,
                                  OnFailureListener onFailure) {
        // Thay vì dùng collectionGroup (cần index), ta sẽ query trực tiếp getAllProjects
        // rồi filter ở client side (tạm thời cho đơn giản)
        // Hoặc có thể lưu array projectIds trong user document

        db.collection(COLLECTION_PROJECTS)
            .whereArrayContains("memberIds", userId) // Cần thêm field này vào Project model
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Project> projects = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    Project project = doc.toObject(Project.class);
                    if (project != null) {
                        project.setProjectId(doc.getId());
                        projects.add(project);
                    }
                }
                Log.d(TAG, "Found " + projects.size() + " projects for user");
                onSuccess.onSuccess(projects);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user projects", e);
                // Fallback: Lấy tất cả projects (temporary solution)
                getAllProjects(onSuccess, onFailure);
            });
    }

    // READ: Lấy TẤT CẢ projects trong database
    public void getAllProjects(OnSuccessListener<List<Project>> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Project> projects = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    Project project = doc.toObject(Project.class);
                    if (project != null) {
                        project.setProjectId(doc.getId());
                        projects.add(project);
                    }
                }
                Log.d(TAG, "Found " + projects.size() + " projects in total");
                onSuccess.onSuccess(projects);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting all projects", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Cập nhật thông tin project
    public void updateProject(String projectId, Map<String, Object> updates,
                             OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Project updated: " + projectId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(onFailure);
    }

    // UPDATE: Đổi tên project
    public void updateProjectName(String projectId, String newName,
                                  OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updateProject(projectId, updates, onSuccess, onFailure);
    }

    // DELETE: Xóa project
    public void deleteProject(String projectId, OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        // Đơn giản: Chỉ xóa project document
        // Sub-collections (members, tasks) sẽ không tự động xóa
        // Nhưng với quy mô nhỏ, cứ để đó cũng OK!
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Project deleted: " + projectId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(onFailure);
    }

    // MEMBERS: Thêm member vào project
    public void addMember(String projectId, ProjectMember member,
                         OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_MEMBERS)
            .document(member.getUserId())
            .set(member)
            .addOnSuccessListener(aVoid -> {
                // Tăng memberCount
                db.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .update("memberCount", FieldValue.increment(1))
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Member added to project: " + projectId);
                        onSuccess.onSuccess(aVoid);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(onFailure);
    }

    // MEMBERS: Lấy tất cả members của project
    public void getProjectMembers(String projectId, OnSuccessListener<List<ProjectMember>> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_MEMBERS)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<ProjectMember> members = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    ProjectMember member = doc.toObject(ProjectMember.class);
                    if (member != null) {
                        members.add(member);
                    }
                }
                Log.d(TAG, "Found " + members.size() + " members in project");
                onSuccess.onSuccess(members);
            })
            .addOnFailureListener(onFailure);
    }

    // MEMBERS: Lấy tất cả members của project với thông tin User đầy đủ
    public void getProjectMembersWithDetails(String projectId,
                                 OnSuccessListener<List<com.fptu.prm392.mad.models.User>> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_MEMBERS)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<com.fptu.prm392.mad.models.User> users = new ArrayList<>();

                for (DocumentSnapshot doc : querySnapshot) {
                    ProjectMember member = doc.toObject(ProjectMember.class);
                    if (member != null) {
                        // Convert ProjectMember to User (ProjectMember đã có đầy đủ thông tin)
                        com.fptu.prm392.mad.models.User user = new com.fptu.prm392.mad.models.User();
                        user.setUserId(member.getUserId());
                        user.setEmail(member.getEmail());
                        user.setFullname(member.getFullname());
                        user.setAvatar(member.getAvatar());
                        users.add(user);
                    }
                }

                Log.d(TAG, "Loaded " + users.size() + " user details from ProjectMembers");
                onSuccess.onSuccess(users);
            })
            .addOnFailureListener(onFailure);
    }

    // MEMBERS: Xóa member khỏi project
    public void removeMember(String projectId, String userId,
                            OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_MEMBERS)
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                // Giảm memberCount
                db.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .update("memberCount", FieldValue.increment(-1))
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Member removed from project: " + projectId);
                        onSuccess.onSuccess(aVoid);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(onFailure);
    }
}
