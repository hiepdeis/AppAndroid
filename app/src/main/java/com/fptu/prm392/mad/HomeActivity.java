package com.fptu.prm392.mad;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;


public class HomeActivity extends AppCompatActivity {

    private EditText edtProjectId;
    private Button btnGoProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        edtProjectId = findViewById(R.id.edtProjectId);
        btnGoProject = findViewById(R.id.btnGoProject);

        btnGoProject.setOnClickListener(v -> {
            String projectId = edtProjectId.getText().toString().trim();
            System.out.println("Project ID = " + projectId);
           //Log.d("DEBUG_HOME", "Project ID = " + projectId);
            if (projectId.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Vui lòng nhập Project ID", Toast.LENGTH_SHORT).show();
            } else {
                // Gửi Project ID sang màn hình khác
                Intent intent = new Intent(HomeActivity.this, ProjectMembersActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            }
        });
        edtProjectId.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnGoProject.performClick(); // tự động bấm nút
                return true;
            }
            return false;
        });
    }
}