package com.fptu.prm392.mad;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Application class để khởi tạo Firebase và enable offline persistence
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Firestore Offline Persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        Log.d(TAG, "Firestore offline persistence enabled");
    }
}

