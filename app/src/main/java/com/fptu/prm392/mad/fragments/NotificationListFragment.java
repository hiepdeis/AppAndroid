package com.fptu.prm392.mad.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.NotificationAdapter;
import com.fptu.prm392.mad.models.Notification;
import com.fptu.prm392.mad.repositories.NotificationRepository;
import com.fptu.prm392.mad.ProjectDetailActivity;
import com.fptu.prm392.mad.TaskDetailActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class NotificationListFragment extends Fragment {

    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private LinearLayout emptyState;
    private TextView tvMarkAllRead;
    private TextView tvUnreadCount;

    private NotificationRepository notificationRepository;
    private FirebaseAuth mAuth;

    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationRepository = new NotificationRepository();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification_list, container, false);

        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        emptyState = view.findViewById(R.id.emptyState);
        tvMarkAllRead = view.findViewById(R.id.tvMarkAllRead);
        tvUnreadCount = view.findViewById(R.id.tvUnreadCount);

        // Setup RecyclerView
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        notificationAdapter = new NotificationAdapter(notification -> {
            // Mark as read when clicked
            if (!notification.isRead()) {
                notificationRepository.markAsRead(
                    notification.getNotificationId(),
                    aVoid -> {
                        // Reload notifications
                        loadNotifications();
                    },
                    e -> {
                        // Ignore error
                    }
                );
            }

            // Navigate based on notification type
            navigateToNotificationTarget(notification);
        });
        recyclerViewNotifications.setAdapter(notificationAdapter);

        // Mark all as read
        tvMarkAllRead.setOnClickListener(v -> {
            notificationRepository.markAllAsRead(
                aVoid -> {
                    Toast.makeText(getContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
                    loadNotifications();
                },
                e -> {
                    Toast.makeText(getContext(), "Error marking notifications as read: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            );
        });

        loadNotifications();

        return view;
    }

    private void loadNotifications() {
        notificationRepository.getAllNotifications(
            notifications -> {
                if (notifications.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerViewNotifications.setVisibility(View.GONE);
                    updateUnreadCount(0);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerViewNotifications.setVisibility(View.VISIBLE);
                    notificationAdapter.setNotifications(notifications);
                    
                    // Count unread notifications
                    int unreadCount = 0;
                    for (Notification notification : notifications) {
                        if (!notification.isRead()) {
                            unreadCount++;
                        }
                    }
                    updateUnreadCount(unreadCount);
                }
            },
            e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading notifications: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void updateUnreadCount(int count) {
        if (tvUnreadCount != null) {
            if (count > 0) {
                tvUnreadCount.setText(String.valueOf(count));
                tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
        }
    }

    private void navigateToNotificationTarget(Notification notification) {
        if (notification.getProjectId() != null && !notification.getProjectId().isEmpty()) {
            Intent intent = new Intent(getContext(), ProjectDetailActivity.class);
            intent.putExtra("PROJECT_ID", notification.getProjectId());
            startActivity(intent);
        } else if (notification.getTaskId() != null && !notification.getTaskId().isEmpty()) {
            Intent intent = new Intent(getContext(), TaskDetailActivity.class);
            intent.putExtra("TASK_ID", notification.getTaskId());
            startActivity(intent);
        }
        // If no projectId or taskId, just mark as read (already done above)
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }
}


