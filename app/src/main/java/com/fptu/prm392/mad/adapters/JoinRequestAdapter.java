package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.ProjectJoinRequest;
import com.fptu.prm392.mad.utils.AvatarLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.ViewHolder> {

    private List<ProjectJoinRequest> requests;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(ProjectJoinRequest request, int position);
        void onReject(ProjectJoinRequest request, int position);
    }

    public JoinRequestAdapter(OnRequestActionListener listener) {
        this.requests = new ArrayList<>();
        this.listener = listener;
    }

    public void setRequests(List<ProjectJoinRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < requests.size()) {
            requests.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_join_request_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectJoinRequest request = requests.get(position);
        holder.bind(request, position);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectName, tvRequesterName, tvRequesterEmail, tvRequestTime;
        ImageView ivRequesterAvatar;
        android.widget.LinearLayout btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvRequesterName = itemView.findViewById(R.id.tvRequesterName);
            tvRequesterEmail = itemView.findViewById(R.id.tvRequesterEmail);
            tvRequestTime = itemView.findViewById(R.id.tvRequestTime);
            ivRequesterAvatar = itemView.findViewById(R.id.ivRequesterAvatar);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(ProjectJoinRequest request, int position) {
            // Check request type
            boolean isInvitation = "invitation".equals(request.getRequestType());

            if (isInvitation) {
                // INVITATION: Manager mời user → Hiển thị thông tin PROJECT
                tvProjectName.setText("Invitation to join: " + request.getProjectName());
                tvRequesterName.setText("Project Manager invited you");
                tvRequesterEmail.setText("Click Accept to join this project");

                // Load project icon instead of user avatar (or default icon)
                ivRequesterAvatar.setImageResource(R.drawable.project);
            } else {
                // JOIN REQUEST: User xin vào → Hiển thị thông tin USER
                tvProjectName.setText("Join request for: " + request.getProjectName());
                tvRequesterName.setText(request.getRequesterName());
                tvRequesterEmail.setText(request.getRequesterEmail());

                // Load user avatar
                AvatarLoader.loadAvatar(itemView.getContext(), request.getRequesterAvatar(), ivRequesterAvatar);
            }

            // Set timestamp
            if (request.getCreatedAt() != null) {
                String timeAgo = getTimeAgo(request.getCreatedAt().toDate());
                tvRequestTime.setText(timeAgo);
            }

            // Accept button
            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(request, position);
                }
            });

            // Reject button
            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(request, position);
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

