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
}

