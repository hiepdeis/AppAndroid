package com.fptu.prm392.mad;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.AddUserAdapter;
import com.fptu.prm392.mad.adapters.MemberAdapter;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MemberListActivity extends AppCompatActivity {

    private ImageView btnBack, btnAddMember;
    private TextView tvTitle;
    private RecyclerView rvMembers;

    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private MemberAdapter memberAdapter;
    private String projectId;
    private String projectOwnerId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_member_list);

        // Get projectId from intent
        projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null) {
            Toast.makeText(this, "Error: Project not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        projectRepository = new ProjectRepository();
        userRepository = new UserRepository();

        // Get current user ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnAddMember = findViewById(R.id.btnAddMember);
        tvTitle = findViewById(R.id.tvTitle);
        rvMembers = findViewById(R.id.rvMembers);

        // Setup RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberAdapter((member, position) ->
            showDeleteMemberConfirmation(member)
        );
        rvMembers.setAdapter(memberAdapter);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Add member button
        btnAddMember.setOnClickListener(v -> showAddMemberDialog());

        // Load project info first to check ownership
        loadProjectInfo();
    }

    private void loadProjectInfo() {
        projectRepository.getProjectById(projectId,
            project -> {
                projectOwnerId = project.getCreatedBy();
                updateUIBasedOnOwnership();
                loadProjectMembers();
            },
            e -> {
                Toast.makeText(this, "Error loading project: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                finish();
            }
        );
    }

    private void updateUIBasedOnOwnership() {
        boolean isManager = currentUserId.equals(projectOwnerId);

        // Hiện/ẩn nút add member
        btnAddMember.setVisibility(isManager ? View.VISIBLE : View.GONE);

        // Set listener cho adapter để enable/disable delete button
        memberAdapter.setIsManager(isManager);
    }

    private void loadProjectMembers() {
        projectRepository.getProjectMembers(projectId,
            members -> {
                memberAdapter.setMembers(members);
            },
            e -> {
                Toast.makeText(this, "Error loading members: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showDeleteMemberConfirmation(ProjectMember member) {
        new AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove " +
                (member.getFullname() != null ? member.getFullname() : member.getEmail()) +
                " from this project?")
            .setPositiveButton("Remove", (dialog, which) -> deleteMember(member))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteMember(ProjectMember member) {
        projectRepository.removeMemberFromProject(projectId, member.getUserId(),
            aVoid -> {
                Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                loadProjectMembers();
            },
            e -> {
                Toast.makeText(this, "Error removing member: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showAddMemberDialog() {
        android.app.Dialog addDialog = new android.app.Dialog(this);
        addDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addDialog.setContentView(R.layout.dialog_add_users);

        // Set dialog width
        Window window = addDialog.getWindow();
        if (window != null) {
            window.setLayout(android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize views
        RecyclerView rvUsers = addDialog.findViewById(R.id.rvUsers);
        EditText etSearchUser = addDialog.findViewById(R.id.etSearchUser);
        TextView tvEmptyUsers = addDialog.findViewById(R.id.tvEmptyUsers);

        // Setup RecyclerView
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        AddUserAdapter addUserAdapter = new AddUserAdapter((AddUserAdapter.OnAddUserListener) user -> {
            addMemberToProject(user, addDialog);
        });
        rvUsers.setAdapter(addUserAdapter);

        // Load users excluding existing members
        loadAvailableUsers(addUserAdapter, rvUsers, tvEmptyUsers);

        // Search functionality
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                addUserAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addDialog.show();
    }

    private void loadAvailableUsers(AddUserAdapter adapter, RecyclerView recyclerView, TextView emptyView) {
        projectRepository.getProjectMembers(projectId,
            members -> {
                List<String> memberUserIds = new ArrayList<>();
                for (ProjectMember member : members) {
                    memberUserIds.add(member.getUserId());
                }

                userRepository.getAllUsers(
                    allUsers -> {
                        List<User> availableUsers = new ArrayList<>();
                        for (User user : allUsers) {
                            if (!memberUserIds.contains(user.getUserId())) {
                                availableUsers.add(user);
                            }
                        }

                        if (availableUsers.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                            adapter.setUsers(availableUsers);
                        }
                    },
                    e -> Toast.makeText(this, "Error loading users: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
                );
            },
            e -> Toast.makeText(this, "Error loading members: " + e.getMessage(),
                Toast.LENGTH_SHORT).show()
        );
    }

    private void addMemberToProject(User user, android.app.Dialog addDialog) {
        // Check if user already sent request
        com.fptu.prm392.mad.repositories.ProjectJoinRequestRepository requestRepo =
            new com.fptu.prm392.mad.repositories.ProjectJoinRequestRepository();

        requestRepo.hasPendingRequest(projectId, user.getUserId(),
            hasPending -> {
                if (hasPending) {
                    Toast.makeText(this, "This user already has a pending request", Toast.LENGTH_LONG).show();
                    return;
                }

                // Get project info first
                projectRepository.getProjectById(projectId,
                    project -> {
                        // Create Invitation Request (manager mời user)
                        com.fptu.prm392.mad.models.ProjectJoinRequest request =
                            new com.fptu.prm392.mad.models.ProjectJoinRequest(
                                null, // requestId will be generated
                                projectId,
                                project.getName(),
                                user.getUserId(),        // requesterId = user được mời
                                user.getFullname(),
                                user.getEmail(),
                                user.getAvatar(),
                                project.getCreatedBy(), // managerId = manager thật sự (người gửi lời mời)
                                "invitation" // manager mời user vào project
                            );

                        // Send invitation
                        requestRepo.createJoinRequest(request,
                            requestId -> {
                                Toast.makeText(this, "Invitation sent to " + user.getFullname(), Toast.LENGTH_LONG).show();
                                addDialog.dismiss();
                            },
                            e -> {
                                Toast.makeText(this, "Error sending invitation: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            }
                        );
                    },
                    e -> {
                        Toast.makeText(this, "Error loading project: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                Toast.makeText(this, "Error checking request: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }
}

