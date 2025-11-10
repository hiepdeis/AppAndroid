package com.fptu.prm392.mad.Domains.Projects.Adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;


import com.fptu.prm392.mad.Domains.Projects.Models.Task;
import com.fptu.prm392.mad.R;

import java.util.ArrayList;
import java.util.List;
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH>{


    public interface OnTaskActionListener {
        void onViewClicked(Task task);
        void onDoneToggled(Task task, boolean newDone);
    }

    private final List<Task> tasks = new ArrayList<>();
    private final OnTaskActionListener listener;

    public TaskAdapter(List<Task> initial, OnTaskActionListener listener) {
        if (initial != null) this.tasks.addAll(initial);
        this.listener = listener;
    }

    public void setTasks(List<Task> newTasks) {
        tasks.clear();
        if (newTasks != null) tasks.addAll(newTasks);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Task t = tasks.get(position);
        holder.tvTitle.setText(t.getTitle() != null ? t.getTitle() : "(No title)");
        holder.tvStatus.setText(t.getStatus() != null ? t.getStatus() : "N/A");
        holder.tvAssignees.setText(t.getAssignees() != null ? String.join(", ", t.getAssignees()) : "");
        holder.cbDone.setOnCheckedChangeListener(null);

        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onViewClicked(t);
        });
//        holder.cbDone.setChecked(t.isDone());
//        holder.btnView.setOnClickListener(v -> {
//            if (listener != null) listener.onViewClicked(t);
//        });
        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null && t.isDone() != isChecked) {
                listener.onDoneToggled(t, isChecked);
            }
        });
    }

    @Override public int getItemCount() {  return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvAssignees;
        Button btnView;
        CheckBox cbDone;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvAssignees = itemView.findViewById(R.id.tvTaskAssignees);
            btnView = itemView.findViewById(R.id.btnViewTask);
            cbDone = itemView.findViewById(R.id.cbTaskDone);
        }
    }


}
