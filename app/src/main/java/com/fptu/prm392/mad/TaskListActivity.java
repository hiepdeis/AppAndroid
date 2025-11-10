package com.fptu.prm392.mad;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.Domains.Projects.Adapters.TaskAdapter;
import com.fptu.prm392.mad.Domains.Projects.Models.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TaskListActivity extends AppCompatActivity {

    private static final String TAG = "TaskListActivity";

    private FirebaseFirestore db;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private ProgressBar progress;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        db = FirebaseFirestore.getInstance();
        rvTasks = findViewById(R.id.rvTasks);
        progress = findViewById(R.id.progress);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onViewClicked(Task task) {
                showTaskDetailDialog(task);
            }

            @Override
            public void onDoneToggled(Task task, boolean newDone) {
                task.setDone(newDone);
                if (task.getTaskId() != null && !task.getTaskId().isEmpty()) {
                    db.collection("tasks").document(task.getTaskId())
                            .update("done", newDone)
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to update done: " + e.getMessage()));
                }
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);

        // accept both keys just in case caller used a different extra name
        String projectId = getIntent().getStringExtra("projectId");
        if (projectId == null || projectId.isEmpty()) {
            // fallback to legacy key
            projectId = getIntent().getStringExtra("PROJECT_QUERY");
        }

        if (projectId != null) {
            Log.d(TAG, "Received projectId: " + projectId);
        } else {
            Log.d(TAG, "No projectId received — loading all tasks");
        }

        loadTasks(projectId);
    }

    private void loadTasks(String projectId) {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvTasks.setVisibility(View.GONE);

        taskList.clear();
        // if TaskAdapter exposes setTasks, we prefer to use it to replace data atomically
        try {
            adapter.setTasks(taskList);
        } catch (Throwable ignored) {
            // fallback if adapter doesn't implement setTasks
            adapter.notifyDataSetChanged();
        }

        Query q = db.collection("tasks");
        if (projectId != null && !projectId.isEmpty()) {
            q = q.whereEqualTo("projectId", projectId);
        }
        q = q.orderBy("status");

        q.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task t = doc.toObject(Task.class);
                        if (t != null) {
                            t.setTaskId(doc.getId());
                            taskList.add(t);
                            Log.d(TAG, "Loaded task: " + t.toString());
                        }
                    }
                    // update adapter safely
                    try {
                        adapter.setTasks(taskList);
                    } catch (Throwable ignored) {
                        adapter.notifyDataSetChanged();
                    }

                    boolean empty = taskList.isEmpty();
                    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvTasks.setVisibility(empty ? View.GONE : View.VISIBLE);

                    if (empty) {
                        Toast.makeText(this, "No tasks found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading tasks: " + e.getMessage());
                    Toast.makeText(this, "Lỗi khi tải tasks: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvTasks.setVisibility(View.GONE);
                })
                .addOnCompleteListener(task -> progress.setVisibility(View.GONE));
    }

    private void showTaskDetailDialog(Task task) {
        String assignees = "";
        if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < task.getAssignees().size(); i++) {
                if (task.getAssignees().get(i) != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(task.getAssignees().get(i));
                }
            }
            assignees = sb.toString();
        }

        String due = task.getDueDate() != null ? task.getDueDate().toString() : "N/A";
        String message = "Title: " + safe(task.getTitle()) +
                "\nStatus: " + safe(task.getStatus()) +
                "\nDue: " + due +
                "\nAssignees: " + assignees +
                "\n\n" + safe(task.getDescription());

        new AlertDialog.Builder(this)
                .setTitle("Task details")
                .setMessage(message)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}