package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;


    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // CREATE: Tạo user mới trong Firestore (sau khi Firebase Auth tạo account)
    public void createUser(User user, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS)
            .document(user.getUserId())
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User created successfully: " + user.getUserId());
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user", e);
                onFailure.onFailure(e);
            });
    }

    public void updateUserAvatar(String userId, String avatarUrl, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        db.collection("users").document(userId)
                .update("avatar", avatarUrl)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }
    // READ: Lấy thông tin user theo ID
    public void getUserById(String userId, OnSuccessListener<User> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setUserId(documentSnapshot.getId());
                        Log.d(TAG, "User found: " + user.getFullname());
                        onSuccess.onSuccess(user);
                    } else {
                        onFailure.onFailure(new Exception("User data is null"));
                    }
                } else {
                    Log.d(TAG, "User not found: " + userId);
                    onFailure.onFailure(new Exception("User not found"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user", e);
                onFailure.onFailure(e);
            });
    }

    // READ: Lấy tất cả users
    public void getAllUsers(OnSuccessListener<List<User>> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS)
            // Không dùng orderBy để tránh lỗi index, sẽ sort ở client side
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<User> users = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setUserId(doc.getId());
                        users.add(user);
                    }
                }

                // Sort by fullname ở client side
                users.sort((u1, u2) -> {
                    String name1 = u1.getFullname() != null ? u1.getFullname().toLowerCase() : "";
                    String name2 = u2.getFullname() != null ? u2.getFullname().toLowerCase() : "";
                    return name1.compareTo(name2);
                });

                Log.d(TAG, "Found " + users.size() + " users");
                onSuccess.onSuccess(users);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting users", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Cập nhật thông tin user
    public void updateUser(String userId, Map<String, Object> updates,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Thêm timestamp update
        updates.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_USERS)
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User updated successfully: " + userId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating user", e);
                onFailure.onFailure(e);
            });
    }

    // UPDATE: Cập nhật tên user
    public void updateUserName(String userId, String newName,
                              OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullname", newName);
        updateUser(userId, updates, onSuccess, onFailure);
    }


    // DELETE: HARD DELETE - Xóa hẳn user khỏi Firebase Auth và Firestore
    public void deleteUserCompletely(OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            onFailure.onFailure(new Exception("Không có user đang đăng nhập"));
            return;
        }

        String userId = currentUser.getUid();

        // Step 1: Xóa từ Firestore trước
        db.collection(COLLECTION_USERS)
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User deleted from Firestore: " + userId);

                // Step 2: Xóa Firebase Auth account
                currentUser.delete()
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "User deleted from Firebase Auth: " + userId);
                        onSuccess.onSuccess(v);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting from Firebase Auth", e);
                        // Firestore đã xóa nhưng Auth không xóa được
                        // Có thể cần re-authenticate
                        onFailure.onFailure(e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting user from Firestore", e);
                onFailure.onFailure(e);
            });
    }

    // DELETE: Xóa user khác (chỉ xóa Firestore, không thể xóa Auth của người khác)
    // Dùng khi admin xóa member khỏi hệ thống
    public void deleteUserById(String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User deleted from Firestore: " + userId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting user", e);
                onFailure.onFailure(e);
            });
    }
}
