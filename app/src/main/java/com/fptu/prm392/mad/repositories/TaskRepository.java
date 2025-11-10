package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Task;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private static final String COLLECTION_TASKS = "tasks";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // READ: Lấy task theo ID
    public void getTaskById(String taskId,
                           OnSuccessListener<Task> onSuccess,
                           OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Task task = documentSnapshot.toObject(Task.class);
                        if (task != null) {
                            Log.d(TAG, "Task found: " + taskId);
                            onSuccess.onSuccess(task);
                        } else {
                            Log.e(TAG, "Task data is null");
                            onFailure.onFailure(new Exception("Task data is null"));
                        }
                    } else {
                        Log.e(TAG, "Task not found: " + taskId);
                        onFailure.onFailure(new Exception("Task not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting task", e);
                    onFailure.onFailure(e);
                });
    }

    // CREATE: Tạo task mới với current user làm owner
    public void createTask(String projectId, String title, String description,
                          OnSuccessListener<String> onSuccess,
                          OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Tạo document reference với ID tự động
        com.google.firebase.firestore.DocumentReference docRef = db.collection(COLLECTION_TASKS).document();
        String taskId = docRef.getId();

        // Tạo object Task với current user làm owner và creator
        Task task = new Task(taskId, projectId, title, description, currentUserId);

        // Lưu vào Firestore
        docRef.set(task)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task created successfully: " + taskId + " with owner: " + currentUserId);
                    onSuccess.onSuccess(taskId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating task", e);
                    onFailure.onFailure(e);
                });
    }

    // Đếm số lượng todo tasks của user trong một project
    public void countMyTodoTasksInProject(String projectId, String userId,
                                          OnSuccessListener<Integer> onSuccess,
                                          OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("status", "todo")
                .whereArrayContains("assignees", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    Log.d(TAG, "Found " + count + " todo tasks for user in project " + projectId);
                    onSuccess.onSuccess(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error counting todo tasks", e);
                    onFailure.onFailure(e);
                });
    }

    // Đếm số lượng pending tasks (todo + in_progress) của user trong một project
    public void countMyPendingTasksInProject(String projectId, String userId,
                                             OnSuccessListener<Integer> onSuccess,
                                             OnFailureListener onFailure) {
        // Get all tasks của user trong project
        db.collection(COLLECTION_TASKS)
                .whereEqualTo("projectId", projectId)
                .whereArrayContains("assignees", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            String status = task.getStatus();
                            // Count todo và in_progress
                            if ("todo".equals(status) || "in_progress".equals(status)) {
                                count++;
                            }
                        }
                    }
                    Log.d(TAG, "Found " + count + " pending tasks (todo + in_progress) for user in project " + projectId);
                    onSuccess.onSuccess(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error counting pending tasks", e);
                    onFailure.onFailure(e);
                });
    }

    // Lấy tất cả tasks của một project
    public void getTasksByProject(String projectId,
                                  OnSuccessListener<List<Task>> onSuccess,
                                  OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .whereEqualTo("projectId", projectId)
                // NOTE: Bỏ orderBy để tránh lỗi missing index
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }

                    // Sort by createdAt descending trên client-side
                    tasks.sort((t1, t2) -> {
                        if (t1.getCreatedAt() == null) return 1;
                        if (t2.getCreatedAt() == null) return -1;
                        return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                    });

                    Log.d(TAG, "Found " + tasks.size() + " tasks in project " + projectId);
                    onSuccess.onSuccess(tasks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting tasks", e);
                    onFailure.onFailure(e);
                });
    }

    // Lấy tasks được assign cho user hiện tại
    public void getMyTasks(OnSuccessListener<List<Task>> onSuccess,
                          OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_TASKS)
                .whereArrayContains("assignees", currentUserId)
                // NOTE: Bỏ orderBy để tránh lỗi missing composite index
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }

                    // Sort by createdAt descending trên client-side
                    tasks.sort((t1, t2) -> {
                        if (t1.getCreatedAt() == null) return 1;
                        if (t2.getCreatedAt() == null) return -1;
                        return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                    });

                    Log.d(TAG, "Found " + tasks.size() + " tasks for current user");
                    onSuccess.onSuccess(tasks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user tasks", e);
                    onFailure.onFailure(e);
                });
    }

    // Interface để trả về task statistics
    public interface TaskStatsCallback {
        void onTaskStats(int pendingCount, int doneCount, int totalCount);
    }

    // Lấy thống kê tasks của project (pending vs done)
    public void getProjectTaskStats(String projectId,
                                    TaskStatsCallback callback,
                                    OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalCount = 0;
                    int doneCount = 0;
                    int pendingCount = 0; // todo + in_progress

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            totalCount++;
                            if (task.isDone()) {
                                doneCount++;
                            } else {
                                pendingCount++; // todo hoặc in_progress
                            }
                        }
                    }

                    Log.d(TAG, String.format("Project %s stats: %d pending, %d done, %d total",
                            projectId, pendingCount, doneCount, totalCount));
                    callback.onTaskStats(pendingCount, doneCount, totalCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting task stats", e);
                    onFailure.onFailure(e);
                });
    }

    // UPDATE: Cập nhật chỉ status của task
    public void updateTaskStatus(String taskId, String status,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task status updated successfully: " + taskId + " to " + status);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task status", e);
                    onFailure.onFailure(e);
                });
    }

    // UPDATE: Cập nhật task với map updates tùy chỉnh
    public void updateTask(String taskId, java.util.Map<String, Object> updates,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully: " + taskId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task", e);
                    onFailure.onFailure(e);
                });
    }

    // UPDATE: Cập nhật status và dueDate của task
    public void updateTaskStatusAndDueDate(String taskId, String status, com.google.firebase.Timestamp dueDate,
                                           OnSuccessListener<Void> onSuccess,
                                           OnFailureListener onFailure) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();

        if (status != null) {
            updates.put("status", status);
        }

        if (dueDate != null) {
            updates.put("dueDate", dueDate);
        }

        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully: " + taskId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task", e);
                    onFailure.onFailure(e);
                });
    }

    // UPDATE: Cập nhật status, dueDate và assignees của task
    public void updateTaskDetails(String taskId, String status, com.google.firebase.Timestamp dueDate,
                                  java.util.List<String> assignees,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();

        if (status != null) {
            updates.put("status", status);
        }

        if (dueDate != null) {
            updates.put("dueDate", dueDate);
        }

        if (assignees != null && !assignees.isEmpty()) {
            updates.put("assignees", assignees);
        }

        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task details updated successfully: " + taskId + " with " +
                            (assignees != null ? assignees.size() : 0) + " assignees");
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task details", e);
                    onFailure.onFailure(e);
                });
    }

    // DELETE: Xóa task
    public void deleteTask(String taskId,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted successfully: " + taskId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting task", e);
                    onFailure.onFailure(e);
                });
    }

    // UPDATE: Thêm assignee vào task
    public void addAssigneeToTask(String taskId, String userId,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) {
                        List<String> assignees = task.getAssignees() != null
                                ? new ArrayList<>(task.getAssignees())
                                : new ArrayList<>();

                        if (!assignees.contains(userId)) {
                            assignees.add(userId);
                            db.collection(COLLECTION_TASKS)
                                    .document(taskId)
                                    .update("assignees", assignees)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Assignee added to task: " + taskId);
                                        onSuccess.onSuccess(aVoid);
                                    })
                                    .addOnFailureListener(onFailure);
                        } else {
                            onSuccess.onSuccess(null);
                        }
                    } else {
                        onFailure.onFailure(new Exception("Task not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    // UPDATE: Xóa assignee khỏi task
    public void removeAssigneeFromTask(String taskId, String userId,
                                      OnSuccessListener<Void> onSuccess,
                                      OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) {
                        List<String> assignees = task.getAssignees() != null
                                ? new ArrayList<>(task.getAssignees())
                                : new ArrayList<>();

                        if (assignees.remove(userId)) {
                            db.collection(COLLECTION_TASKS)
                                    .document(taskId)
                                    .update("assignees", assignees)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Assignee removed from task: " + taskId);
                                        onSuccess.onSuccess(aVoid);
                                    })
                                    .addOnFailureListener(onFailure);
                        } else {
                            onSuccess.onSuccess(null);
                        }
                    } else {
                        onFailure.onFailure(new Exception("Task not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }
}

