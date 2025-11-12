package com.fptu.prm392.mad;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.GlobalSearchAdapter;
import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.models.GlobalSearchResult;
import com.fptu.prm392.mad.models.Project;
import com.fptu.prm392.mad.models.Task;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ChatRepository;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.TaskRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalSearchActivity extends AppCompatActivity {

    private static final String TAG = "GlobalSearchActivity";
    private static final int DEBOUNCE_DELAY_MS = 300;

    private SearchView searchView;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvEmptyMessage;
    private ImageView btnClose;

    private GlobalSearchAdapter adapter;
    private Handler searchHandler;
    private Runnable searchRunnable;

    // Repositories
    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ChatRepository chatRepository;
    private FirebaseAuth auth;

    // Cache for user's projects
    private List<Project> myProjects;
    private Set<String> myProjectIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_search);

        // Initialize repositories
        projectRepository = new ProjectRepository();
        taskRepository = new TaskRepository();
        userRepository = new UserRepository();
        chatRepository = new ChatRepository();
        auth = FirebaseAuth.getInstance();

        searchHandler = new Handler(Looper.getMainLooper());
        myProjectIds = new HashSet<>();

        // Initialize views
        searchView = findViewById(R.id.searchView);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnClose = findViewById(R.id.btnClose);

        // Setup RecyclerView
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GlobalSearchAdapter(new GlobalSearchAdapter.OnItemClickListener() {
            @Override
            public void onProjectClick(Project project, boolean isUserMember) {
                handleProjectClick(project, isUserMember);
            }

            @Override
            public void onTaskClick(Task task) {
                handleTaskClick(task);
            }

            @Override
            public void onUserClick(User user) {
                handleUserClick(user);
            }
        });
        rvSearchResults.setAdapter(adapter);

        // Close button
        btnClose.setOnClickListener(v -> finish());

        // Setup search
        setupSearch();

        // Load user's projects for reference
        loadMyProjects();

        // Show empty state initially
        showEmptyState("Start typing to search");
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (newText.trim().isEmpty()) {
                    adapter.clearResults();
                    showEmptyState("Start typing to search");
                    return true;
                }

                // Debounce 300ms
                searchRunnable = () -> performSearch(newText);
                searchHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
                return true;
            }
        });
    }

    private void loadMyProjects() {
        projectRepository.getMyProjects(
            projects -> {
                myProjects = projects;
                myProjectIds.clear();
                for (Project project : projects) {
                    myProjectIds.add(project.getProjectId());
                }
                Log.d(TAG, "Loaded " + myProjects.size() + " user projects");
            },
            e -> Log.e(TAG, "Error loading user projects", e)
        );
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            return;
        }

        showLoading();

        // Search in parallel: Projects (all), Tasks (my tasks), Users (in my projects)
        List<GlobalSearchResult> allResults = new ArrayList<>();
        final int[] completedQueries = {0};
        final int totalQueries = 3;

        // 1. Search all public projects
        projectRepository.searchAllPublicProjects(query,
            publicProjects -> {
                List<GlobalSearchResult> projectResults = new ArrayList<>();
                for (Project project : publicProjects) {
                    boolean isUserMember = myProjectIds.contains(project.getProjectId());
                    projectResults.add(new GlobalSearchResult(project, isUserMember));
                }

                if (!projectResults.isEmpty()) {
                    allResults.add(new GlobalSearchResult("PROJECTS", projectResults.size()));
                    allResults.addAll(projectResults);
                }

                completedQueries[0]++;
                if (completedQueries[0] == totalQueries) {
                    displayResults(allResults);
                }
            },
            e -> {
                Log.e(TAG, "Error searching projects", e);
                completedQueries[0]++;
                if (completedQueries[0] == totalQueries) {
                    displayResults(allResults);
                }
            }
        );

        // 2. Search my tasks
        taskRepository.searchMyTasks(query,
            tasks -> {
                List<GlobalSearchResult> taskResults = new ArrayList<>();
                for (Task task : tasks) {
                    taskResults.add(new GlobalSearchResult(task));
                }

                if (!taskResults.isEmpty()) {
                    allResults.add(new GlobalSearchResult("TASKS", taskResults.size()));
                    allResults.addAll(taskResults);
                }

                completedQueries[0]++;
                if (completedQueries[0] == totalQueries) {
                    displayResults(allResults);
                }
            },
            e -> {
                Log.e(TAG, "Error searching tasks", e);
                completedQueries[0]++;
                if (completedQueries[0] == totalQueries) {
                    displayResults(allResults);
                }
            }
        );

        // 3. Search users in my projects
        searchUsersInMyProjects(query, allResults, completedQueries, totalQueries);
    }

    private void searchUsersInMyProjects(String query, List<GlobalSearchResult> allResults,
                                        int[] completedQueries, int totalQueries) {
        if (myProjects == null || myProjects.isEmpty()) {
            completedQueries[0]++;
            if (completedQueries[0] == totalQueries) {
                displayResults(allResults);
            }
            return;
        }

        // Collect all member IDs from my projects
        Set<String> allMemberIdsSet = new HashSet<>();
        for (Project project : myProjects) {
            if (project.getMemberIds() != null) {
                allMemberIdsSet.addAll(project.getMemberIds());
            }
        }

        List<String> allMemberIds = new ArrayList<>(allMemberIdsSet);

        userRepository.searchUsersInMyProjects(allMemberIds, query,
            users -> {
                List<GlobalSearchResult> userResults = new ArrayList<>();
                for (User user : users) {
                    userResults.add(new GlobalSearchResult(user));
                }

                if (!userResults.isEmpty()) {
                    allResults.add(new GlobalSearchResult("USERS", userResults.size()));
                    allResults.addAll(userResults);
                }

                completedQueries[0]++;
                if (completedQueries[0] == totalQueries) {
                    displayResults(allResults);
                }
            },
            e -> {
                Log.e(TAG, "Error searching users", e);
                completedQueries[0]++;
                if (completedQueries[0] == totalQueries) {
                    displayResults(allResults);
                }
            }
        );
    }

    private void displayResults(List<GlobalSearchResult> results) {
        hideLoading();

        if (results.isEmpty()) {
            showEmptyState("No results found");
        } else {
            adapter.setResults(results);
            rvSearchResults.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleProjectClick(Project project, boolean isUserMember) {
        if (isUserMember) {
            // Open ProjectDetailActivity
            Intent intent = new Intent(this, ProjectDetailActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        } else {
            // Show mini project detail dialog (like dialog_project_detail_mini)
            showProjectMiniDetail(project);
        }
    }

    private void handleTaskClick(Task task) {
        // Open TaskDetailActivity
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("TASK_ID", task.getTaskId());
        startActivity(intent);
    }

    private void handleUserClick(User user) {
        // Show user profile with chat option
        showUserProfileDialog(user);
    }

    private void showProjectMiniDetail(Project project) {
        // TODO: Implement dialog similar to dialog_project_detail_mini
        // For now, just show a simple toast
        Toast.makeText(this, "Project: " + project.getName() + " (Public)", Toast.LENGTH_SHORT).show();

        // You can show the existing join project dialog here
        // Or create a new mini detail dialog
    }

    private void showUserProfileDialog(User user) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_user_profile);
        dialog.getWindow().setLayout(
            (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
            (int) (getResources().getDisplayMetrics().heightPixels * 0.7)
        );

        // Set user info
        TextView tvFullname = dialog.findViewById(R.id.tvProfileFullname);
        TextView tvEmail = dialog.findViewById(R.id.tvProfileEmail);
        ImageView ivAvatar = dialog.findViewById(R.id.ivProfileAvatar);
        ImageView btnBack = dialog.findViewById(R.id.btnBack);

        tvFullname.setText(user.getDisplayName());
        tvEmail.setText(user.getEmail());
        com.fptu.prm392.mad.utils.AvatarLoader.loadAvatar(this, user.getAvatar(), ivAvatar);

        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Add Chat button (you need to add this to layout or overlay it)
        // For now, handle click on the profile to open chat
        dialog.findViewById(R.id.tvProfileFullname).setOnClickListener(v -> {
            openChatWithUser(user);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openChatWithUser(User user) {
        showLoading();

        chatRepository.getOrCreateOneOnOneChat(
            user.getUserId(),
            user.getDisplayName(),
            chat -> {
                hideLoading();
                // Open chat in HomeActivity
                Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra("OPEN_CHAT_ID", chat.getChatId());
                intent.putExtra("CHAT_NAME", user.getDisplayName());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            },
            e -> {
                hideLoading();
                Toast.makeText(this, "Error opening chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        emptyState.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvEmptyMessage.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}

