package com.fptu.prm392.mad.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.fptu.prm392.mad.HomeActivity;
import com.fptu.prm392.mad.ProjectMembersActivity;
import com.fptu.prm392.mad.R;

/**
 * Helper class để hiển thị Local Notifications
 * Không cần Firebase Cloud Messaging
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "project_notifications";
    private static final String CHANNEL_NAME = "Project Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for project updates, tasks, and member activities";

    /**
     * Tạo notification channel (bắt buộc cho Android 8.0+)
     * Gọi method này trong Application.onCreate() hoặc HomeActivity.onCreate()
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Hiển thị notification về project
     * @param context Context
     * @param title Tiêu đề notification
     * @param content Nội dung notification
     * @param projectId ID của project (để mở khi click)
     */
    public static void showNotification(Context context, String title, String content, String projectId) {
        // Tạo intent để mở ProjectMembersActivity khi click vào notification
        Intent intent = new Intent(context, ProjectMembersActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Tạo notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Sử dụng icon có sẵn
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Tự động xóa khi user click
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content)); // Hiển thị text dài hơn

        NotificationManager manager = (NotificationManager) 
            context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (manager != null) {
            // Sử dụng timestamp làm ID để mỗi notification là unique
            int notificationId = (int) System.currentTimeMillis();
            manager.notify(notificationId, builder.build());
        }
    }

    /**
     * Hiển thị notification về task
     * @param context Context
     * @param title Tiêu đề notification
     * @param content Nội dung notification
     * @param projectId ID của project
     * @param taskId ID của task (nếu có)
     */
    public static void showTaskNotification(Context context, String title, String content, 
                                           String projectId, String taskId) {
        Intent intent = new Intent(context, ProjectMembersActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        if (taskId != null) {
            intent.putExtra("TASK_ID", taskId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        NotificationManager manager = (NotificationManager) 
            context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (manager != null) {
            int notificationId = (int) System.currentTimeMillis();
            manager.notify(notificationId, builder.build());
        }
    }

    /**
     * Kiểm tra xem notification permission đã được cấp chưa
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            return manager != null && manager.areNotificationsEnabled();
        }
        return true; // Android < 13 không cần permission
    }
}

