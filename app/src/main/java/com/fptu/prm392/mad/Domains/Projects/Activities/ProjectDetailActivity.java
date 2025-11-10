package com.fptu.prm392.mad.Domains.Projects.Activities;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.Domains.Projects.Adapters.MemberAdapter;
import com.fptu.prm392.mad.Domains.Projects.Adapters.UserAdapter;
import com.fptu.prm392.mad.Domains.Projects.Dtos.UserAdd;
import com.fptu.prm392.mad.Domains.Projects.Dtos.UserDto;
import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.databinding.ActivityProjectDetailBinding;

import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import android.app.AlertDialog;
import androidx.annotation.Nullable;

import com.fptu.prm392.mad.Domains.Projects.Models.Project;
import com.fptu.prm392.mad.Domains.Projects.Dtos.Member;
import com.fptu.prm392.mad.Domains.Projects.Models.User;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.google.firebase.firestore.SetOptions;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class ProjectDetailActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener{
    private ActivityProjectDetailBinding binding;
    private FirebaseFirestore db;
    private List<UserDto> memberList = new ArrayList<>();
    private MemberAdapter adapter;

    private Button btnAddMember;
    private List<UserAdd> allUsersAdd = new ArrayList<>();
    private UserAdapter adapter1;
    private String currentProjectId;
    private AlertDialog userPickerDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new MemberAdapter(memberList, member -> handleRemoveRequest(member));
        // Setup RecyclerView
        binding.rvMemberList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMemberList.setAdapter(adapter);

        // Nhận dữ liệu project
        Project project;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            project = getIntent().getParcelableExtra("PROJECT_DETAIL", Project.class);
        } else {
            project = getIntent().getParcelableExtra("PROJECT_DETAIL");
        }

        if (project != null) {
            displayProjectDetails(project);
            loadMembersFromFirestore(project.getMemberIds());
            currentProjectId = project.getProjectId();
        }

        btnAddMember = findViewById(R.id.btnAddMember1);
        btnAddMember.setOnClickListener(v -> showUserPickerDialog());
        fetchUsers();


    }

    private void displayProjectDetails(Project project) {
        binding.tvProjectName.setText("Tên Project: " + project.getName());
        binding.tvProjectId.setText("ID: " + project.getProjectId());
        binding.tvProjectDescription.setText("Mô tả: " + project.getDescription());
        binding.tvMemberCount.setText("Số thành viên: " + project.getMemberCount());
    }

    private void loadMembersFromFirestore(List<Member> memberListFromProject) {
        if (memberListFromProject == null || memberListFromProject.isEmpty()) return;

        for (Member member : memberListFromProject) {
            String memberId = member.getUserId();
            String role = member.getRole(); // lấy luôn role để gán cho user hiển thị

            db.collection("users").document(memberId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // ✅ Tạo đối tượng UserDto từ User + Member
                                UserDto userDto = new UserDto(
                                        user.getUserId(),
                                        user.getDisplayName(),
                                        user.getEmail(),
                                        user.getFullname(),
                                        user.getAvatar(),
                                        role // role từ Member
                                );

                                memberList.add(userDto); // list này đổi kiểu sang List<UserDto>
                                adapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Lỗi khi lấy user " + memberId + ": " + e.getMessage()));




        }
    }


    private void fetchUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsersAdd.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String fullname = doc.contains("fullname") ? doc.getString("fullname") : "";
                        String email = doc.contains("email") ? doc.getString("email") : "";
                        allUsersAdd.add(new UserAdd(id, fullname != null ? fullname : "", email != null ? email : ""));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }


    private void showUserPickerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.activity_add_member, null);

        EditText etSearch = dialogView.findViewById(R.id.etSearchUser);
        RecyclerView rvUsers = dialogView.findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with current user list
        adapter1 = new UserAdapter(new ArrayList<>(allUsersAdd), this);
        rvUsers.setAdapter(adapter1);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select user")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter1.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
        userPickerDialog = dialog;
    }


    @Override
    public void onUserClick(UserAdd user) {
        // Handle selected user: add to project or return result
        Toast.makeText(this, "Selected: " + user.getFullname(), Toast.LENGTH_SHORT).show();

        String selectedId = user.getId(); // adjust to actual getter if different
        for (UserDto u : memberList) {
            if (u.getUserId().equals(selectedId)) {
                Toast.makeText(this, "User is already a member", Toast.LENGTH_SHORT).show();
                if (userPickerDialog != null && userPickerDialog.isShowing()) userPickerDialog.dismiss();
                return;
            }
        }

        // prepare map to add into Firestore array (memberIds contains maps with userId + role)
        Map<String, Object> newMemberMap = new HashMap<>();
        newMemberMap.put("userId", selectedId);
        newMemberMap.put("role", "member");

        db.collection("projects").document(currentProjectId)
                .update("memberIds", FieldValue.arrayUnion(newMemberMap))
                .addOnSuccessListener(aVoid -> {
                    // Update local UI list (create UserDto with role = "member")
                    UserDto userDto = new UserDto(
                            selectedId,
                            "", // displayName unknown from UserAdd
                            user.getEmail() != null ? user.getEmail() : "",
                            user.getFullname() != null ? user.getFullname() : "",
                            null, // avatar unknown
                            "member"
                    );
                    memberList.add(userDto);
                    adapter.notifyDataSetChanged();
                    binding.tvMemberCount.setText("Số thành viên: " + memberList.size());
                    Toast.makeText(this, "User added as member", Toast.LENGTH_SHORT).show();
                    if (userPickerDialog != null && userPickerDialog.isShowing()) userPickerDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        // Example: add user id to project members, or call your existing method
    }
    private void handleRemoveRequest(UserDto member) {

        String projectIdToCheck = currentProjectId;
        db.collection("tasks")
                .whereEqualTo("projectId", projectIdToCheck)
                .whereArrayContains("assignees", member.getUserId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) showConfirmRemoveDialog(member);
                    else showConfirmBanDialog(member);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to check tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showConfirmRemoveDialog(UserDto member) {
        new AlertDialog.Builder(this)
                .setTitle("User has tasks")
                .setMessage(member.getFullname() + " has assigned tasks. Ban instead?")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Ban", (d, w) -> removeMemberFromProject(member))
                .show();
    }

    private void showConfirmBanDialog(UserDto member) {
        new AlertDialog.Builder(this)
                .setTitle("User has tasks")
                .setMessage(member.getFullname() + " has assigned tasks. You cannot remove them. Ban instead?")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Ban", (d, w) -> banMemberInProject(member))
                .show();
    }

    private void removeMemberFromProject(UserDto member) {
        db.collection("projects").document(currentProjectId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<Map<String, Object>> memberIds = (List<Map<String, Object>>) doc.get("memberIds");
                    if (memberIds == null) memberIds = new ArrayList<>();
                    List<Map<String, Object>> newList = new ArrayList<>();
                    for (Map<String, Object> m : memberIds) {
                        if (!member.getUserId().equals(m.get("userId"))) newList.add(m);
                    }
                    db.collection("projects").document(currentProjectId)
                            .set(Map.of("memberIds", newList), SetOptions.merge())
                            .addOnSuccessListener(a -> removeMemberLocally(member.getUserId()));
                });
    }




    private void banMemberInProject(UserDto member) {
        db.collection("projects").document(currentProjectId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<Map<String, Object>> memberIds = (List<Map<String, Object>>) doc.get("memberIds");
                    if (memberIds == null) memberIds = new ArrayList<>();
                    boolean found = false;
                    List<Map<String, Object>> newList = new ArrayList<>();
                    for (Map<String, Object> m : memberIds) {
                        if (member.getUserId().equals(m.get("userId"))) {
                            Map<String,Object> changed = new HashMap<>(m);
                            changed.put("role","banned");
                            newList.add(changed);
                            found = true;
                        } else newList.add(m);
                    }
                    if (!found) newList.add(Map.of("userId", member.getUserId(), "role", "banned"));
                    db.collection("projects").document(currentProjectId)
                            .set(Map.of("memberIds", newList), SetOptions.merge())
                            .addOnSuccessListener(a -> changeMemberRoleLocally(member.getUserId(), "banned"));
                });
    }




    private void removeMemberLocally(String userId){
        for (int i=0;i<memberList.size();i++){
            if (memberList.get(i).getUserId().equals(userId)){
                memberList.remove(i);
                adapter.notifyItemRemoved(i);
                binding.tvMemberCount.setText("Số thành viên: " + memberList.size());
                break;
            }
        }
    }

    private void changeMemberRoleLocally(String userId,String role){
        for (int i=0;i<memberList.size();i++){
            if (memberList.get(i).getUserId().equals(userId)){
                memberList.get(i).setRole(role);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

}