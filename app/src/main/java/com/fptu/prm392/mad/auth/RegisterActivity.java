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
    private TextInputEditText etFullname, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Ánh xạ các view
        etFullname = findViewById(R.id.etFullname);
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
        String fullname = etFullname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Reset errors
        etFullname.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);

        // Validate Fullname - chỉ cho phép chữ cái và khoảng trắng
        if (TextUtils.isEmpty(fullname)) {
            etFullname.setError("Vui lòng nhập họ và tên");
            etFullname.requestFocus();
            return;
        }

        if (!fullname.matches("^[a-zA-ZÀ-ỹ\\s]+$")) {
            etFullname.setError("Họ tên chỉ được chứa chữ cái");
            etFullname.requestFocus();
            return;
        }

        if (fullname.length() < 2) {
            etFullname.setError("Họ tên phải có ít nhất 2 ký tự");
            etFullname.requestFocus();
            return;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không đúng định dạng");
            etEmail.requestFocus();
            return;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Hiển thị progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Kiểm tra email đã tồn tại trong Firebase Auth hay chưa
        // Bằng cách thử đăng ký - Firebase sẽ tự động check duplicate
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Đăng ký Firebase Auth thành công
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String userId = firebaseUser.getUid();

                        // Tạo User object với fullname từ input
                        User newUser = new User(
                            userId,
                            email,
                            fullname,  // Sử dụng fullname từ form thay vì tách từ email
                            null  // avatar = null
                        );

                        // Lưu vào Firestore
                        userRepository.createUser(newUser,
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

                                // Xóa user khỏi Firebase Auth nếu lưu Firestore thất bại
                                firebaseUser.delete();
                            }
                        );
                    } else {
                        // Đăng ký thất bại
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Lỗi không xác định";

                        if (errorMessage.contains("email address is already in use")) {
                            etEmail.setError("Email này đã được sử dụng");
                            etEmail.requestFocus();
                            Toast.makeText(RegisterActivity.this, "Email này đã được đăng ký!",
                                    Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("badly formatted")) {
                            etEmail.setError("Email không đúng định dạng");
                            etEmail.requestFocus();
                            Toast.makeText(RegisterActivity.this, "Email không hợp lệ!",
                                    Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("password")) {
                            etPassword.setError("Mật khẩu không hợp lệ");
                            etPassword.requestFocus();
                            Toast.makeText(RegisterActivity.this, "Mật khẩu không hợp lệ!",
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

