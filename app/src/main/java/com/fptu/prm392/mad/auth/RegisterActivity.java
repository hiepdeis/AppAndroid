package com.fptu.prm392.mad.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fptu.prm392.mad.HomeActivity;
import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các view
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        // Xử lý sự kiện click đăng ký
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Xử lý sự kiện chuyển đến màn hình đăng nhập
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Quay lại MainActivity
            }
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Kiểm tra dữ liệu nhập vào
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        // Hiển thị progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Đăng ký với Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Đăng ký Firebase Auth thành công
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String userId = firebaseUser.getUid();

                        // Tạo User object (KHÔNG CÓ roleId nữa)
                        User newUser = new User(
                            userId,
                            email,
                            email.split("@")[0], // Dùng phần trước @ làm fullname tạm
                            null  // avatar = null
                        );

                        // Lưu vào Firestore
                        UserRepository userRepo = new UserRepository();
                        userRepo.createUser(newUser,
                            success -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);

                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!",
                                        Toast.LENGTH_SHORT).show();

                                // Chuyển đến HomeActivity
                                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                finish();
                            },
                            error -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);

                                Toast.makeText(RegisterActivity.this,
                                    "Lỗi lưu thông tin: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            }
                        );
                    } else {
                        // Đăng ký thất bại
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        String errorMessage = task.getException().getMessage();
                        if (errorMessage.contains("email address is already in use")) {
                            Toast.makeText(RegisterActivity.this, "Email này đã được sử dụng!",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
    }
}
