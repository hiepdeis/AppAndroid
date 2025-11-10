package com.fptu.prm392.mad.Domains.Tasks.Repo;
import androidx.annotation.NonNull;

import com.fptu.prm392.mad.Domains.Projects.Models.Task;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;
import com.fptu.prm392.mad.Domains.Tasks.Interfaces.TasksCallback;
import com.google.firebase.Timestamp;
public class TaskRepository {
    private final FirebaseFirestore db;

    public TaskRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Sửa lại hàm này để chạy BẤT ĐỒNG BỘ.
     * Nó không trả về "List" nữa, mà sẽ "báo" kết quả về qua "callback".
     */
    public void getTasksByUserIdAndMonth(String userId, Timestamp startOfMonth, Timestamp endOfMonth, final TasksCallback callback) {
        List<Task> tasks = new ArrayList<>();

        db.collection("tasks")
                .whereArrayContains("assignees", userId) // Lọc theo user
                .whereGreaterThanOrEqualTo("dueDate", startOfMonth) // Lọc theo ngày bắt đầu
                .whereLessThanOrEqualTo("dueDate", endOfMonth) // Lọc theo ngày kết thúc
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Task task = doc.toObject(Task.class);
                            if (task != null) {
                                task.setTaskId(doc.getId());
                                tasks.add(task);
                            }
                        }
                    }
                    callback.onSuccess(tasks);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onFailure(e);
                });
    }


    /**
     * HÀM CŨ: (Vẫn giữ lại nếu bạn cần, hoặc có thể xóa đi)
     * Lấy TẤT CẢ task của userId (Không tối ưu cho Calendar)
     */
    public void getTasksByUserId(String userId, final TasksCallback callback) {
        List<Task> tasks = new ArrayList<>();

        db.collection("tasks")
                .whereArrayContains("assignees", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Task task = doc.toObject(Task.class);
                            if (task != null) {
                                task.setTaskId(doc.getId());
                                tasks.add(task);
                            }
                        }
                    }
                    callback.onSuccess(tasks);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onFailure(e);
                });
    }
}
