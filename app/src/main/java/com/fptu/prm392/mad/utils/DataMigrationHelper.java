package com.fptu.prm392.mad.utils;

import android.util.Log;

import com.fptu.prm392.mad.repositories.ProjectRepository;

/**
 * Helper class to fix corrupted data in Firestore
 * Use this once to fix memberIds field from HashMap to List<String>
 */
public class DataMigrationHelper {
    private static final String TAG = "DataMigration";

    /**
     * Fix all projects with corrupted memberIds field
     * Call this method once from any Activity (e.g., HomeActivity.onCreate)
     *
     * Example usage:
     * DataMigrationHelper.fixProjectsMemberIds();
     */
    public static void fixProjectsMemberIds() {
        ProjectRepository projectRepository = new ProjectRepository();

        projectRepository.fixAllProjectsMemberIds(
            result -> {
                Log.i(TAG, "Migration completed: " + result);
            },
            error -> {
                Log.e(TAG, "Migration failed: " + error.getMessage());
            }
        );
    }
}

