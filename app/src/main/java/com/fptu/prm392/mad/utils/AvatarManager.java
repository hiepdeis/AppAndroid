package com.fptu.prm392.mad.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AvatarManager {
    private static final String TAG = "AvatarManager";
    private static final String AVATARS_FOLDER = "avatars";
    private static final int MAX_IMAGE_SIZE = 800; // pixels
    private static final int JPEG_QUALITY = 80;

    private final FirebaseStorage storage;
    private final Context context;

    public AvatarManager(Context context) {
        this.context = context;
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Upload avatar image to Firebase Storage
     * @param userId User ID (used as folder name)
     * @param imageUri Uri of selected image
     * @param onSuccess Callback with download URL
     * @param onFailure Callback on error
     */
    public void uploadAvatar(String userId, Uri imageUri,
                            OnSuccessListener<String> onSuccess,
                            OnFailureListener onFailure) {
        try {
            // Compress image
            byte[] imageData = compressImage(imageUri);

            // Create storage reference
            StorageReference avatarRef = storage.getReference()
                    .child(AVATARS_FOLDER)
                    .child(userId)
                    .child("avatar.jpg");

            // Upload image
            UploadTask uploadTask = avatarRef.putBytes(imageData);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "Avatar uploaded successfully: " + downloadUrl);
                    onSuccess.onSuccess(downloadUrl);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    onFailure.onFailure(e);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload avatar", e);
                onFailure.onFailure(e);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            onFailure.onFailure(e);
        }
    }

    /**
     * Compress image to reduce file size
     * @param imageUri Uri of image to compress
     * @return Compressed image as byte array
     */
    private byte[] compressImage(Uri imageUri) throws IOException {
        // Read image from URI
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("Cannot open image input stream");
        }

        // Decode image
        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        if (originalBitmap == null) {
            throw new IOException("Cannot decode image");
        }

        // Resize if needed
        Bitmap resizedBitmap = resizeBitmap(originalBitmap);

        // Compress to JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] data = baos.toByteArray();

        // Clean up
        if (resizedBitmap != originalBitmap) {
            resizedBitmap.recycle();
        }
        originalBitmap.recycle();
        baos.close();

        Log.d(TAG, "Compressed image size: " + data.length + " bytes");
        return data;
    }

    /**
     * Resize bitmap if it's too large
     * @param bitmap Original bitmap
     * @return Resized bitmap (or original if already small enough)
     */
    private Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Check if resize is needed
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap;
        }

        // Calculate new dimensions
        float scale;
        if (width > height) {
            scale = (float) MAX_IMAGE_SIZE / width;
        } else {
            scale = (float) MAX_IMAGE_SIZE / height;
        }

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Log.d(TAG, "Resizing image from " + width + "x" + height +
                   " to " + newWidth + "x" + newHeight);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Delete user's avatar from Firebase Storage
     * @param userId User ID
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    public void deleteAvatar(String userId,
                            OnSuccessListener<Void> onSuccess,
                            OnFailureListener onFailure) {
        StorageReference avatarRef = storage.getReference()
                .child(AVATARS_FOLDER)
                .child(userId)
                .child("avatar.jpg");

        avatarRef.delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}

