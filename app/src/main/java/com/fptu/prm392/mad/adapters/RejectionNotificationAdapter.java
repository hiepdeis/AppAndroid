package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RejectionNotificationAdapter extends RecyclerView.Adapter<RejectionNotificationAdapter.ViewHolder> {

    private List<Notification> notifications = new ArrayList<>();
    private final OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onDismiss(Notification notification, int position);
    }

    public RejectionNotificationAdapter(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_rejection_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position), position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNotificationTitle, tvNotificationMessage, tvNotificationTime;
        ImageView btnDismissNotification;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            btnDismissNotification = itemView.findViewById(R.id.btnDismissNotification);
        }

        public void bind(Notification notification, int position) {
            tvNotificationTitle.setText(notification.getTitle());
            tvNotificationMessage.setText(notification.getMessage());

            // Set timestamp
            if (notification.getCreatedAt() != null) {
                String timeAgo = getTimeAgo(notification.getCreatedAt().toDate());
                tvNotificationTime.setText(timeAgo);
            }

            // Dismiss button
            btnDismissNotification.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDismiss(notification, position);
                }
            });
        }

        private String getTimeAgo(Date date) {
            long diff = System.currentTimeMillis() - date.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
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

