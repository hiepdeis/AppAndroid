package com.fptu.prm392.mad.adapters;

import android.content.ClipData;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.ViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;
    private OnTaskDragListener dragListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskDragListener {
        void onTaskDragStarted(Task task);
    }

    public TaskCardAdapter(OnTaskClickListener listener, OnTaskDragListener dragListener) {
        this.tasks = new ArrayList<>();
        this.listener = listener;
        this.dragListener = dragListener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
        // Sort by dueDate - nearest deadline first
        this.tasks.sort((t1, t2) -> {
            // Tasks without due date go to the end
            if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
            if (t1.getDueDate() == null) return 1;
            if (t2.getDueDate() == null) return -1;
            // Compare due dates - earlier dates first
            return t1.getDueDate().compareTo(t2.getDueDate());
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
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
        TextView tvTaskTitle, tvDueDate;
        LinearLayout layoutDueDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            layoutDueDate = itemView.findViewById(R.id.layoutDueDate);
        }

        public void bind(Task task) {
            // Set task title
            tvTaskTitle.setText(task.getTitle());

            // Set due date if available
            if (task.getDueDate() != null) {
                tvDueDate.setText(dateFormat.format(task.getDueDate().toDate()));
                layoutDueDate.setVisibility(View.VISIBLE);
            } else {
                layoutDueDate.setVisibility(View.GONE);
            }

            // Handle click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            // Handle long click to start drag
            itemView.setOnLongClickListener(v -> {
                // Create clip data with task ID
                ClipData data = ClipData.newPlainText("taskId", task.getTaskId());

                // Create drag shadow
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);

                // Start drag
                v.startDragAndDrop(data, shadowBuilder, task, 0);

                // Notify listener
                if (dragListener != null) {
                    dragListener.onTaskDragStarted(task);
                }

                return true;
            });
        }
    }
}

