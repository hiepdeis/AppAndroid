package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.GlobalSearchResult;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.utils.AvatarLoader;

import java.util.ArrayList;
import java.util.List;

public class GlobalSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PROJECT = 1;
    private static final int TYPE_TASK = 2;
    private static final int TYPE_USER = 3;

    private List<GlobalSearchResult> results;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onProjectClick(Project project, boolean isUserMember);
        void onTaskClick(Task task);
        void onUserClick(User user);
    }

    public GlobalSearchAdapter(OnItemClickListener listener) {
        this.results = new ArrayList<>();
        this.listener = listener;
    }

    public void setResults(List<GlobalSearchResult> results) {
        this.results = results;
        notifyDataSetChanged();
    }

    public void clearResults() {
        this.results.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        GlobalSearchResult.ResultType type = results.get(position).getType();
        switch (type) {
            case HEADER:
                return TYPE_HEADER;
            case PROJECT:
                return TYPE_PROJECT;
            case TASK:
                return TYPE_TASK;
            case USER:
                return TYPE_USER;
            default:
                return TYPE_HEADER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_global_search_header, parent, false);
                return new HeaderViewHolder(headerView);
            case TYPE_PROJECT:
                View projectView = inflater.inflate(R.layout.item_global_search_project, parent, false);
                return new ProjectViewHolder(projectView);
            case TYPE_TASK:
                View taskView = inflater.inflate(R.layout.item_global_search_task, parent, false);
                return new TaskViewHolder(taskView);
            case TYPE_USER:
                View userView = inflater.inflate(R.layout.item_global_search_user, parent, false);
                return new UserViewHolder(userView);
            default:
                View defaultView = inflater.inflate(R.layout.item_global_search_header, parent, false);
                return new HeaderViewHolder(defaultView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GlobalSearchResult result = results.get(position);

        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                ((HeaderViewHolder) holder).bind(result);
                break;
            case TYPE_PROJECT:
                ((ProjectViewHolder) holder).bind(result);
                break;
            case TYPE_TASK:
                ((TaskViewHolder) holder).bind(result);
                break;
            case TYPE_USER:
                ((UserViewHolder) holder).bind(result);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    // Header ViewHolder
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle, tvResultCount;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
            tvResultCount = itemView.findViewById(R.id.tvResultCount);
        }

        public void bind(GlobalSearchResult result) {
            tvHeaderTitle.setText(result.getHeaderTitle());
            tvResultCount.setText(String.valueOf(result.getResultCount()));
        }
    }

    // Project ViewHolder
    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectName, tvProjectDescription, tvMembershipBadge;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvProjectDescription = itemView.findViewById(R.id.tvProjectDescription);
            tvMembershipBadge = itemView.findViewById(R.id.tvMembershipBadge);
        }

        public void bind(GlobalSearchResult result) {
            Project project = result.getProject();
            boolean isUserMember = result.isUserMember();

            tvProjectName.setText(project.getName());
            tvProjectDescription.setText(project.getDescription());

            if (isUserMember) {
                tvMembershipBadge.setText("Joined");
                tvMembershipBadge.setBackgroundResource(R.drawable.bg_status_done);
            } else {
                tvMembershipBadge.setText("Public");
                tvMembershipBadge.setBackgroundResource(R.drawable.bg_status_in_progress);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project, isUserMember);
                }
            });
        }
    }

    // Task ViewHolder
    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskDescription, tvTaskStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
        }

        public void bind(GlobalSearchResult result) {
            Task task = result.getTask();

            tvTaskTitle.setText(task.getTitle());
            tvTaskDescription.setText(task.getDescription());

            // Set status badge
            String status = task.getStatus();
            if ("done".equals(status)) {
                tvTaskStatus.setText("DONE");
                tvTaskStatus.setBackgroundResource(R.drawable.bg_status_done);
            } else if ("in_progress".equals(status)) {
                tvTaskStatus.setText("IN PROGRESS");
                tvTaskStatus.setBackgroundResource(R.drawable.bg_status_in_progress);
            } else {
                tvTaskStatus.setText("TODO");
                tvTaskStatus.setBackgroundResource(R.drawable.bg_status_todo);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }
    }

    // User ViewHolder
    class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName, tvUserEmail;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        }

        public void bind(GlobalSearchResult result) {
            User user = result.getUser();

            tvUserName.setText(user.getDisplayName());
            tvUserEmail.setText(user.getEmail());

            // Load avatar
            AvatarLoader.loadAvatar(itemView.getContext(), user.getAvatar(), ivUserAvatar);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}

