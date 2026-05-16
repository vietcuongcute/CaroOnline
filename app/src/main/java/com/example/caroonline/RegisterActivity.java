package com.example.caroonline;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtUsername, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvGoLogin;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");

        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            edtUsername.setError("Vui lòng nhập tên người chơi");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu nhập lại không khớp");
            return;
        }

        btnRegister.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = firebaseAuth.getCurrentUser().getUid();

                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", uid);
                    userMap.put("username", username);
                    userMap.put("email", email);
                    userMap.put("aiTotalMatches", 0);
                    userMap.put("aiWin", 0);
                    userMap.put("aiLose", 0);
                    userMap.put("aiDraw", 0);


                    userMap.put("onlineTotalMatches", 0);
                    userMap.put("onlineWin", 0);
                    userMap.put("onlineLose", 0);
                    userMap.put("onlineDraw", 0);
                    userMap.put("onlineScore", 0);

                    usersRef.child(uid).setValue(userMap)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Lỗi lưu user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Lỗi đăng ký: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}