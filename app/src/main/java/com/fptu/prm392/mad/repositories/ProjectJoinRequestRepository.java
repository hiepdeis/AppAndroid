package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.ProjectJoinRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProjectJoinRequestRepository {
    private static final String TAG = "JoinRequestRepo";
    private static final String COLLECTION_JOIN_REQUESTS = "project_join_requests";

    private final FirebaseFirestore db;

    public ProjectJoinRequestRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // CREATE: Tạo join request
    public void createJoinRequest(ProjectJoinRequest request,
                                 OnSuccessListener<String> onSuccess,
                                 OnFailureListener onFailure) {
        String requestId = db.collection(COLLECTION_JOIN_REQUESTS).document().getId();
        request.setRequestId(requestId);

        db.collection(COLLECTION_JOIN_REQUESTS)
            .document(requestId)
            .set(request)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Join request created: " + requestId);
                onSuccess.onSuccess(requestId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating join request", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy pending requests cho một manager
    public void getPendingRequestsForManager(String managerId,
                                            OnSuccessListener<List<ProjectJoinRequest>> onSuccess,
                                            OnFailureListener onFailure) {
        db.collection(COLLECTION_JOIN_REQUESTS)
            .whereEqualTo("managerId", managerId)
            .whereEqualTo("status", "pending")
            // NOTE: Bỏ orderBy để tránh lỗi missing composite index
            // Sẽ sort trên client-side
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<ProjectJoinRequest> requests = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot) {
                    ProjectJoinRequest request = doc.toObject(ProjectJoinRequest.class);
                    if (request != null) {
                        requests.add(request);
                    }
                }

                // Sort by createdAt descending trên client-side
                requests.sort((r1, r2) -> {
                    if (r1.getCreatedAt() == null) return 1;
                    if (r2.getCreatedAt() == null) return -1;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });

                Log.d(TAG, "Found " + requests.size() + " pending requests");
                onSuccess.onSuccess(requests);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting pending requests", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Approve request
    public void approveRequest(String requestId,
                              OnSuccessListener<Void> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION_JOIN_REQUESTS)
            .document(requestId)
            .update(
                "status", "approved",
                "updatedAt", Timestamp.now()
            )
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Request approved: " + requestId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error approving request", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Reject request
    public void rejectRequest(String requestId,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        db.collection(COLLECTION_JOIN_REQUESTS)
            .document(requestId)
            .update(
                "status", "rejected",
                "updatedAt", Timestamp.now()
            )
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Request rejected: " + requestId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error rejecting request", e);
                onFailure.onFailure(e);
            });
    }

    // CHECK: Kiểm tra xem user đã gửi request cho project chưa
    public void hasPendingRequest(String projectId, String userId,
                                 OnSuccessListener<Boolean> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_JOIN_REQUESTS)
            .whereEqualTo("projectId", projectId)
            .whereEqualTo("requesterId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                boolean hasPending = !querySnapshot.isEmpty();
                onSuccess.onSuccess(hasPending);
            })
            .addOnFailureListener(onFailure);
    }

    // DELETE: Xóa request (sau khi approve/reject)
    public void deleteRequest(String requestId,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        db.collection(COLLECTION_JOIN_REQUESTS)
            .document(requestId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Request deleted: " + requestId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting request", e);
                onFailure.onFailure(e);
            });
    }

    // REALTIME: Listen to pending requests for manager
    public com.google.firebase.firestore.ListenerRegistration listenToPendingRequests(
            String managerId,
            OnSuccessListener<List<ProjectJoinRequest>> onDataChanged,
            OnFailureListener onFailure) {

        return db.collection(COLLECTION_JOIN_REQUESTS)
            .whereEqualTo("managerId", managerId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening to requests", error);
                    onFailure.onFailure(error);
                    return;
                }

                if (querySnapshot != null) {
                    List<ProjectJoinRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        ProjectJoinRequest request = doc.toObject(ProjectJoinRequest.class);
                        if (request != null) {
                            requests.add(request);
                        }
                    }

                    // Sort by createdAt descending
                    requests.sort((r1, r2) -> {
                        if (r1.getCreatedAt() == null) return 1;
                        if (r2.getCreatedAt() == null) return -1;
                        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                    });

                    Log.d(TAG, "Realtime update: " + requests.size() + " pending requests");
                    onDataChanged.onSuccess(requests);
                }
            });
    }

    // COUNT: Đếm số pending requests (cho badge)
    public void getPendingRequestCount(String managerId,
                                      OnSuccessListener<Integer> onSuccess,
                                      OnFailureListener onFailure) {
        db.collection(COLLECTION_JOIN_REQUESTS)
            .whereEqualTo("managerId", managerId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int count = querySnapshot.size();
                Log.d(TAG, "Pending request count: " + count);
                onSuccess.onSuccess(count);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error counting requests", e);
                onFailure.onFailure(e);
            });
    }

    // REALTIME: Listen to pending request count (cho badge realtime)
    public com.google.firebase.firestore.ListenerRegistration listenToPendingRequestCount(
            String managerId,
            OnSuccessListener<Integer> onCountChanged,
            OnFailureListener onFailure) {

        return db.collection(COLLECTION_JOIN_REQUESTS)
            .whereEqualTo("managerId", managerId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening to count", error);
                    onFailure.onFailure(error);
                    return;
                }

                if (querySnapshot != null) {
                    int count = querySnapshot.size();
                    Log.d(TAG, "Realtime count update: " + count);
                    onCountChanged.onSuccess(count);
                }
            });
    }
}

