package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Notification;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository để quản lý Notifications trong Firestore
 */
public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Lưu notification vào Firestore
     */
    public void saveNotificationToFirestore(String userId, String type, String title, 
                                           String content, String projectId, String taskId,
                                           OnSuccessListener<String> onSuccess,
                                           OnFailureListener onFailure) {
        // Tạo document reference với ID tự động
        DocumentReference docRef = db.collection(COLLECTION_NOTIFICATIONS).document();
        String notificationId = docRef.getId();

        Notification notification = new Notification(
            notificationId, userId, type, title, content, projectId, taskId
        );

        docRef.set(notification)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notification saved: " + notificationId);
                onSuccess.onSuccess(notificationId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving notification", e);
                onFailure.onFailure(e);
            });
    }

    /**
     * Lấy tất cả notifications chưa đọc của user hiện tại
     */
    public void getUnreadNotifications(OnSuccessListener<List<Notification>> onSuccess,
                                      OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isRead", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Notification> notifications = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Notification notification = doc.toObject(Notification.class);
                    if (notification != null) {
                        notification.setNotificationId(doc.getId());
                        notifications.add(notification);
                    }
                }
                Log.d(TAG, "Found " + notifications.size() + " unread notifications");
                onSuccess.onSuccess(notifications);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting unread notifications", e);
                onFailure.onFailure(e);
            });
    }

    /**
     * Lấy tất cả notifications của user (cả đã đọc và chưa đọc)
     */
    public void getAllNotifications(OnSuccessListener<List<Notification>> onSuccess,
                                  OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50) // Giới hạn 50 notifications gần nhất
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Notification> notifications = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Notification notification = doc.toObject(Notification.class);
                    if (notification != null) {
                        notification.setNotificationId(doc.getId());
                        notifications.add(notification);
                    }
                }
                Log.d(TAG, "Found " + notifications.size() + " notifications");
                onSuccess.onSuccess(notifications);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting notifications", e);
                onFailure.onFailure(e);
            });
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    public void markAsRead(String notificationId,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);

        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notification marked as read: " + notificationId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error marking notification as read", e);
                onFailure.onFailure(e);
            });
    }

    /**
     * Đánh dấu tất cả notifications của user là đã đọc
     */
    public void markAllAsRead(OnSuccessListener<Void> onSuccess,
                              OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentReference> refs = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    refs.add(doc.getReference());
                }

                // Batch update
                if (refs.isEmpty()) {
                    onSuccess.onSuccess(null);
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("isRead", true);

                AtomicInteger completed = new AtomicInteger(0);
                int total = refs.size();
                for (DocumentReference ref : refs) {
                    ref.update(updates)
                        .addOnSuccessListener(aVoid -> {
                            int currentCompleted = completed.incrementAndGet();
                            if (currentCompleted == total) {
                                Log.d(TAG, "All notifications marked as read");
                                onSuccess.onSuccess(aVoid);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating notification", e);
                            int currentCompleted = completed.get();
                            if (currentCompleted == total - 1) {
                                onFailure.onFailure(e);
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting notifications", e);
                onFailure.onFailure(e);
            });
    }

    /**
     * Xóa notification
     */
    public void deleteNotification(String notificationId,
                                   OnSuccessListener<Void> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notification deleted: " + notificationId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting notification", e);
                onFailure.onFailure(e);
            });
    }
}

