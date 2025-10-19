package com.fptu.prm392.mad.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.models.User;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskActionListener listener;
    private Map<String, Boolean> expandedStates = new HashMap<>();
    private boolean canEditTask = false; // Flag to check if user can edit

    public interface OnTaskActionListener {
        void onAddAssigneeClick(Task task, int position);
        void onRemoveAssigneeClick(Task task, String userId, int position);
        void loadAssigneeInfo(String userId, AssigneeInfoCallback callback);
        void onStatusClick(Task task, int position); // NEW - Click to change status
    }

    public interface AssigneeInfoCallback {
        void onUserLoaded(User user);
    }

    public TaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setCanEditTask(boolean canEdit) {
        this.canEditTask = canEdit;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskDescription.setText(task.getDescription());

        // Set status badge
        String status = task.getStatus();
        if (status != null) {
            switch (status) {
                case "todo":
                    holder.tvTaskStatus.setText("TODO");
                    holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_todo);
                    break;
                case "in_progress":
                    holder.tvTaskStatus.setText("ĐANG LÀM");
                    holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_in_progress);
                    break;
                case "done":
                    holder.tvTaskStatus.setText("HOÀN THÀNH");
                    holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_done);
                    break;
                default:
                    holder.tvTaskStatus.setText("TODO");
                    holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_todo);
            }
        }

        // Make status badge clickable if user can edit
        if (canEditTask) {
            holder.tvTaskStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStatusClick(task, position);
                }
            });
        } else {
            holder.tvTaskStatus.setOnClickListener(null);
        }

        // Set assignee count
        int assigneeCount = task.getAssignees() != null ? task.getAssignees().size() : 0;
        holder.tvAssigneeCount.setText(assigneeCount + " người");

        // Set due date
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDueDate.setText(sdf.format(task.getDueDate().toDate()));
            holder.tvDueDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDueDate.setVisibility(View.GONE);
        }

        // Handle expand/collapse state
        String taskId = task.getTaskId();
        boolean isExpanded = expandedStates.getOrDefault(taskId, false);
        holder.layoutExpandable.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpandCollapse.setRotation(isExpanded ? 180 : 0);

        // Click to expand/collapse
        holder.itemView.setOnClickListener(v -> {
            boolean newState = !expandedStates.getOrDefault(taskId, false);
            expandedStates.put(taskId, newState);
            notifyItemChanged(position);
        });

        holder.ivExpandCollapse.setOnClickListener(v -> {
            boolean newState = !expandedStates.getOrDefault(taskId, false);
            expandedStates.put(taskId, newState);
            notifyItemChanged(position);
        });

        // Load and display assignees
        if (isExpanded) {
            displayAssignees(holder, task, position);
        }

        // Add assignee button
        holder.btnAddAssignee.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddAssigneeClick(task, position);
            }
        });
    }

    private void displayAssignees(TaskViewHolder holder, Task task, int position) {
        holder.layoutAssignees.removeAllViews();

        List<String> assigneeIds = task.getAssignees();
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            holder.tvNoAssignees.setVisibility(View.VISIBLE);
            holder.layoutAssignees.setVisibility(View.GONE);
        } else {
            holder.tvNoAssignees.setVisibility(View.GONE);
            holder.layoutAssignees.setVisibility(View.VISIBLE);

            for (String userId : assigneeIds) {
                View assigneeView = LayoutInflater.from(context).inflate(
                        R.layout.item_assignee, holder.layoutAssignees, false);

                TextView tvName = assigneeView.findViewById(R.id.tvAssigneeName);
                TextView tvEmail = assigneeView.findViewById(R.id.tvAssigneeEmail);
                ImageButton btnRemove = assigneeView.findViewById(R.id.btnRemoveAssignee);

                // Load user info
                if (listener != null) {
                    listener.loadAssigneeInfo(userId, user -> {
                        if (user != null) {
                            tvName.setText(user.getDisplayName());
                            tvEmail.setText(user.getEmail());
                        } else {
                            tvName.setText("User ID: " + userId);
                            tvEmail.setText("Không tìm thấy thông tin");
                        }
                    });
                }

                // Remove button
                btnRemove.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveAssigneeClick(task, userId, position);
                    }
                });

                holder.layoutAssignees.addView(assigneeView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskDescription, tvTaskStatus, tvAssigneeCount, tvDueDate;
        ImageView ivExpandCollapse;
        LinearLayout layoutExpandable, layoutAssignees;
        Button btnAddAssignee;
        TextView tvNoAssignees;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvAssigneeCount = itemView.findViewById(R.id.tvAssigneeCount);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            ivExpandCollapse = itemView.findViewById(R.id.ivExpandCollapse);
            layoutExpandable = itemView.findViewById(R.id.layoutExpandable);
            layoutAssignees = itemView.findViewById(R.id.layoutAssignees);
            btnAddAssignee = itemView.findViewById(R.id.btnAddAssignee);
            tvNoAssignees = itemView.findViewById(R.id.tvNoAssignees);
        }
    }
}
