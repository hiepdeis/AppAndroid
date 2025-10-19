    package com.fptu.prm392.mad;
    import android.view.View;
    import android.os.Bundle;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import android.widget.AdapterView;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;

    import androidx.appcompat.app.AlertDialog;
    import java.util.ArrayList;
    import java.util.List;
    import Domains.projectMembers.Adapters.ProjectMemberAdapter;
    import Domains.projectMembers.Dtos.ProjectMember;

    public class ProjectMembersActivity extends AppCompatActivity {
        RecyclerView recyclerMembers;
        ProjectMemberAdapter adapter;
        List<ProjectMember> memberList;
        Button btnAddMember;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_project_members);

            String projectId = getIntent().getStringExtra("PROJECT_ID");
            System.out.println(">>> ProjectMembersActivity nhận được Project ID = " + projectId);
            System.out.println("oke");

            recyclerMembers = findViewById(R.id.recyclerMembers);
            recyclerMembers.setLayoutManager(new LinearLayoutManager(this));
            btnAddMember = findViewById(R.id.btnAddMember);
            // Giả lập dữ liệu (thay bằng API sau)
            memberList = new ArrayList<>();
            memberList.add(new ProjectMember(1, "Pham Hiep", "trungthanh26148@gmail.com", "MANAGER", "ACTIVE"));
            memberList.add(new ProjectMember(5, "test4", "test4@gmail.com", "DEV1", "ACTIVE"));

            adapter = new ProjectMemberAdapter(memberList);
            recyclerMembers.setAdapter(adapter);

            btnAddMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddMemberDialog();
                }
            });
        }

        // 👉 Hàm hiển thị popup nhập thông tin thành viên mới
        private void showAddMemberDialog() {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_member, null);

            EditText edtSearchUser = dialogView.findViewById(R.id.edtSearchUser);
            RecyclerView recyclerSearchResults = dialogView.findViewById(R.id.recyclerSearchResults);
            recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));

            List<ProjectMember> allUsers = new ArrayList<>();
            allUsers.add(new ProjectMember(10, "Nguyen Van A", "a@gmail.com", "DEV", "ACTIVE"));
            allUsers.add(new ProjectMember(11, "Tran Thi B", "b@gmail.com", "TESTER", "ACTIVE"));
            allUsers.add(new ProjectMember(12, "Pham Hiep", "trungthanh26148@gmail.com", "MANAGER", "ACTIVE"));

            // ✅ Tạo dialog TRƯỚC để dùng được trong lambda
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add New Member");
            builder.setView(dialogView);
            builder.setNegativeButton("Close", (d, w) -> d.dismiss());
            final AlertDialog dialog = builder.create(); // phải final để dùng trong callback

            // ✅ Adapter với callback khi chọn item)
            ProjectMemberAdapter searchAdapter = new ProjectMemberAdapter(allUsers,  (ProjectMember member) -> {
                boolean exists = false;
                for (ProjectMember m : memberList) {
                    if (m.getEmail().equals(member.getEmail())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    memberList.add(member);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Đã thêm " + member.getFullname(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Thành viên này đã có trong danh sách!", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss(); // đóng popup
            });

            recyclerSearchResults.setAdapter(searchAdapter);
            dialog.show();

            // 🔍 Tìm kiếm theo tên hoặc email
            edtSearchUser.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(android.text.Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().toLowerCase();
                    List<ProjectMember> filtered = new ArrayList<>();

                    for (ProjectMember m : allUsers) {
                        if (m.getFullname().toLowerCase().contains(query) ||
                                m.getEmail().toLowerCase().contains(query)) {
                            filtered.add(m);
                        }
                    }

                    searchAdapter.updateData(filtered);
                }
            });
        }

    }
