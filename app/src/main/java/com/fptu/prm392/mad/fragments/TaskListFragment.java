package com.fptu.prm392.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.TaskListAdapter;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.repositories.TaskRepository;

public class TaskListFragment extends Fragment {

    private RecyclerView recyclerViewTasks;
    private TaskListAdapter taskListAdapter;
    private LinearLayout emptyStateTask;

    private TaskRepository taskRepository;

    private OnTaskClickListener taskClickListener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public static TaskListFragment newInstance() {
        return new TaskListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskRepository = new TaskRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks);
        emptyStateTask = view.findViewById(R.id.emptyStateTask);

        // Setup RecyclerView
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskListAdapter = new TaskListAdapter(task -> {
            if (taskClickListener != null) {
                taskClickListener.onTaskClick(task);
            }
        });
        recyclerViewTasks.setAdapter(taskListAdapter);

        loadTasks();

        return view;
    }

    private void loadTasks() {
        taskRepository.getMyTasks(
                tasks -> {
                    if (tasks.isEmpty()) {
                        emptyStateTask.setVisibility(View.VISIBLE);
                        recyclerViewTasks.setVisibility(View.GONE);
                    } else {
                        emptyStateTask.setVisibility(View.GONE);
                        recyclerViewTasks.setVisibility(View.VISIBLE);
                        taskListAdapter.setTasks(tasks);
                    }
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading tasks: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.taskClickListener = listener;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }
}

