package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Notification;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationRepository {
    private static final String TAG = "NotificationRepo";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore db;

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // CREATE: Tạo notification
    public void createNotification(Notification notification,
                                  OnSuccessListener<String> onSuccess,
                                  OnFailureListener onFailure) {
        String notificationId = db.collection(COLLECTION_NOTIFICATIONS).document().getId();
        notification.setNotificationId(notificationId);

        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .set(notification)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notification created: " + notificationId);
                onSuccess.onSuccess(notificationId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating notification", e);
                onFailure.onFailure(e);
            });
    }

    // Helper: Tạo notification cho request rejected
    public void createRequestRejectedNotification(String userId, String projectName, String projectId,
                                                 OnSuccessListener<String> onSuccess,
                                                 OnFailureListener onFailure) {
        Notification notification = new Notification(
            null,
            userId,
            "request_rejected",
            "Join Request Rejected",
            "Your request to join \"" + projectName + "\" was rejected.",
            projectId,
            projectName
        );
        createNotification(notification, onSuccess, onFailure);
    }

    // Helper: Tạo notification cho invitation rejected
    public void createInvitationRejectedNotification(String managerId, String userName,
                                                     String projectName, String projectId,
                                                     OnSuccessListener<String> onSuccess,
                                                     OnFailureListener onFailure) {
        Notification notification = new Notification(
            null,
            managerId,
            "invitation_rejected",
            "Invitation Declined",
            userName + " declined your invitation to join \"" + projectName + "\".",
            projectId,
            projectName
        );
        createNotification(notification, onSuccess, onFailure);
    }

    // REALTIME: Listen to rejection notifications for user
    public com.google.firebase.firestore.ListenerRegistration listenToRejectionNotifications(
            String userId,
            OnSuccessListener<java.util.List<Notification>> onDataChanged,
            OnFailureListener onFailure) {

        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening to rejection notifications", error);
                    onFailure.onFailure(error);
                    return;
                }

                if (querySnapshot != null) {
                    java.util.List<Notification> notifications = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notifications.add(notification);
                        }
                    }

                    // Sort by createdAt descending
                    notifications.sort((n1, n2) -> {
                        if (n1.getCreatedAt() == null) return 1;
                        if (n2.getCreatedAt() == null) return -1;
                        return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                    });

                    Log.d(TAG, "Realtime rejection notifications update: " + notifications.size());
                    onDataChanged.onSuccess(notifications);
                }
            });
    }

    // Mark notification as read
    public void markAsRead(String notificationId,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notification marked as read: " + notificationId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error marking notification as read", e);
                onFailure.onFailure(e);
            });
    }
}


