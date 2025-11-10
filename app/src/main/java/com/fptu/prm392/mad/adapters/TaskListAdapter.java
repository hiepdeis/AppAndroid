package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskListAdapter(OnTaskClickListener listener) {
        this.tasks = new ArrayList<>();
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
        // Sort by status first (todo, in_progress, done), then by dueDate within each status
        this.tasks.sort((t1, t2) -> {
            // First, compare by status priority: todo(0) < in_progress(1) < done(2)
            int status1Priority = getStatusPriority(t1.getStatus());
            int status2Priority = getStatusPriority(t2.getStatus());

            if (status1Priority != status2Priority) {
                return Integer.compare(status1Priority, status2Priority);
            }

            // If same status, sort by dueDate - nearest deadline first
            // Tasks without due date go to the end within their status group
            if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
            if (t1.getDueDate() == null) return 1;
            if (t2.getDueDate() == null) return -1;
            // Compare due dates - earlier dates first
            return t1.getDueDate().compareTo(t2.getDueDate());
        });
        notifyDataSetChanged();
    }

    private int getStatusPriority(String status) {
        if ("todo".equals(status)) return 0;
        if ("in_progress".equals(status)) return 1;
        if ("done".equals(status)) return 2;
        return 3; // Unknown status goes last
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskStatus, tvDueDate, tvParticipantCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvParticipantCount = itemView.findViewById(R.id.tvParticipantCount);
        }

        public void bind(Task task) {
            // Set task title
            tvTaskTitle.setText(task.getTitle());

            // Set task status with appropriate styling
            String status = task.getStatus();
            if ("todo".equals(status)) {
                tvTaskStatus.setText("To Do");
                tvTaskStatus.setBackgroundResource(R.drawable.status_badge_todo);
            } else if ("in_progress".equals(status)) {
                tvTaskStatus.setText("In Progress");
                tvTaskStatus.setBackgroundResource(R.drawable.status_badge_in_progress);
            } else if ("done".equals(status)) {
                tvTaskStatus.setText("Done");
                tvTaskStatus.setBackgroundResource(R.drawable.status_badge_done);
            }

            // Set due date
            if (task.getDueDate() != null) {
                tvDueDate.setText(dateFormat.format(task.getDueDate().toDate()));
            } else {
                tvDueDate.setText("No due date");
            }

            // Set participant count
            int participantCount = task.getAssignees() != null ? task.getAssignees().size() : 0;
            tvParticipantCount.setText(String.valueOf(participantCount));

            // Handle click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }
    }
}

