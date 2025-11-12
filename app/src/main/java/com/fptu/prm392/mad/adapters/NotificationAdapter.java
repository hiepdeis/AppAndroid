package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Notification;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.notifications = new ArrayList<>();
        this.listener = listener;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvNotificationIcon, tvNotificationTitle, tvNotificationContent, tvNotificationTime;
        View viewUnreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotificationIcon = itemView.findViewById(R.id.tvNotificationIcon);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationContent = itemView.findViewById(R.id.tvNotificationContent);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }

        public void bind(Notification notification) {
            tvNotificationTitle.setText(notification.getTitle());
            tvNotificationContent.setText(notification.getContent());

            // Set icon based on type
            String icon = getIconForType(notification.getType());
            tvNotificationIcon.setText(icon);

            // Show/hide unread indicator
            if (notification.isRead()) {
                viewUnreadIndicator.setVisibility(View.GONE);
                itemView.setAlpha(0.7f);
            } else {
                viewUnreadIndicator.setVisibility(View.VISIBLE);
                itemView.setAlpha(1.0f);
            }

            // Set time
            if (notification.getCreatedAt() != null) {
                String timeText = formatTime(notification.getCreatedAt().toDate());
                tvNotificationTime.setText(timeText);
            } else {
                tvNotificationTime.setText("");
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        private String getIconForType(String type) {
            if (type == null) return "🔔";
            switch (type) {
                case "task_assigned":
                    return "📋";
                case "task_updated":
                    return "✏️";
                case "task_deleted":
                    return "🗑️";
                case "member_added":
                    return "➕";
                case "member_removed":
                    return "➖";
                case "project_created":
                    return "📁";
                case "project_updated":
                    return "✏️";
                case "project_deleted":
                    return "🗑️";
                case "sync_success":
                    return "✅";
                default:
                    return "🔔";
            }
        }

        private String formatTime(Date date) {
            if (date == null) return "";
            
            long now = System.currentTimeMillis();
            long time = date.getTime();
            long diff = now - time;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                if (days == 1) return "1 day ago";
                if (days < 7) return days + " days ago";
                // More than a week, show actual date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return sdf.format(date);
            } else if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just now";
            }
        }
    }
}



