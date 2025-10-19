package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Task;
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

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private static final String COLLECTION_PROJECTS = "projects";
    private static final String SUBCOLLECTION_TASKS = "tasks";

    private final FirebaseFirestore db;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // CREATE: Tạo task mới trong project
    public void createTask(Task task, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        String projectId = task.getProjectId();

        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .add(task)
            .addOnSuccessListener(documentReference -> {
                String taskId = documentReference.getId();
                task.setTaskId(taskId);

                // Tăng taskCount của project
                db.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .update("taskCount", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Task created: " + taskId);
                        onSuccess.onSuccess(taskId);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating task", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy task theo ID
    public void getTaskById(String projectId, String taskId,
                           OnSuccessListener<Task> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .document(taskId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) {
                        task.setTaskId(documentSnapshot.getId());
                        onSuccess.onSuccess(task);
                    } else {
                        onFailure.onFailure(new Exception("Task data is null"));
                    }
                } else {
                    onFailure.onFailure(new Exception("Task not found"));
                }
            })
            .addOnFailureListener(onFailure);
    }

    // READ: Lấy tất cả tasks của project
    public void getTasksByProject(String projectId, OnSuccessListener<List<Task>> onSuccess,
                                  OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Task> tasks = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    Task task = doc.toObject(Task.class);
                    if (task != null) {
                        task.setTaskId(doc.getId());
                        tasks.add(task);
                    }
                }
                Log.d(TAG, "Found " + tasks.size() + " tasks in project");
                onSuccess.onSuccess(tasks);
            })
            .addOnFailureListener(onFailure);
    }

    // READ: Lấy tasks theo status
    public void getTasksByStatus(String projectId, String status,
                                 OnSuccessListener<List<Task>> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .whereEqualTo("status", status)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Task> tasks = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    Task task = doc.toObject(Task.class);
                    if (task != null) {
                        task.setTaskId(doc.getId());
                        tasks.add(task);
                    }
                }
                onSuccess.onSuccess(tasks);
            })
            .addOnFailureListener(onFailure);
    }

    // READ: Lấy tasks được assign cho user
    public void getTasksAssignedToUser(String projectId, String userId,
                                       OnSuccessListener<List<Task>> onSuccess,
                                       OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .whereArrayContains("assignees", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Task> tasks = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    Task task = doc.toObject(Task.class);
                    if (task != null) {
                        task.setTaskId(doc.getId());
                        tasks.add(task);
                    }
                }
                Log.d(TAG, "Found " + tasks.size() + " tasks assigned to user");
                onSuccess.onSuccess(tasks);
            })
            .addOnFailureListener(onFailure);
    }

    // UPDATE: Cập nhật task
    public void updateTask(String projectId, String taskId, Map<String, Object> updates,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .document(taskId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Task updated: " + taskId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(onFailure);
    }

    // UPDATE: Cập nhật task với Task object
    public void updateTask(Task task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(task.getProjectId())
            .collection(SUBCOLLECTION_TASKS)
            .document(task.getTaskId())
            .set(task)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Task updated: " + task.getTaskId());
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(onFailure);
    }

    // UPDATE: Đổi tên task
    public void updateTaskTitle(String projectId, String taskId, String newTitle,
                               OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updateTask(projectId, taskId, updates, onSuccess, onFailure);
    }

    // UPDATE: Đổi status task
    public void updateTaskStatus(String projectId, String taskId, String newStatus,
                                OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updateTask(projectId, taskId, updates, onSuccess, onFailure);
    }

    // UPDATE: Thêm assignee vào task
    public void addAssignee(String projectId, String taskId, String userId,
                           OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .document(taskId)
            .update("assignees", FieldValue.arrayUnion(userId))
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Assignee added to task: " + taskId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(onFailure);
    }

    // UPDATE: Xóa assignee khỏi task
    public void removeAssignee(String projectId, String taskId, String userId,
                              OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .document(taskId)
            .update("assignees", FieldValue.arrayRemove(userId))
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Assignee removed from task: " + taskId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(onFailure);
    }

    // DELETE: Xóa task
    public void deleteTask(String projectId, String taskId,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .document(taskId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                // Giảm taskCount của project
                db.collection(COLLECTION_PROJECTS)
                    .document(projectId)
                    .update("taskCount", FieldValue.increment(-1))
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Task deleted: " + taskId);
                        onSuccess.onSuccess(aVoid);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(onFailure);
    }

    // HELPER: Đếm số tasks theo status
    public void countTasksByStatus(String projectId, String status,
                                   OnSuccessListener<Integer> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection(COLLECTION_PROJECTS)
            .document(projectId)
            .collection(SUBCOLLECTION_TASKS)
            .whereEqualTo("status", status)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                onSuccess.onSuccess(querySnapshot.size());
            })
            .addOnFailureListener(onFailure);
    }
}
