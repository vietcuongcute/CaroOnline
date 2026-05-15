package com.example.caroonline;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserInfo, tvStats;
    private Button btnPlayAI, btnPlayOnline, btnHistory, btnRanking, btnLogout;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference usersRef;

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");

        initViews();
        loadUserInfo();
        setupEvents();
    }

    private void initViews() {
        tvUserInfo = findViewById(R.id.tvUserInfo);
        tvStats = findViewById(R.id.tvStats);

        btnPlayAI = findViewById(R.id.btnPlayAI);
        btnPlayOnline = findViewById(R.id.btnPlayOnline);
        btnHistory = findViewById(R.id.btnHistory);
        btnRanking = findViewById(R.id.btnRanking);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserInfo() {
        String uid = currentUser.getUid();

        usersRef.child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);

                        Long win = snapshot.child("win").getValue(Long.class);
                        Long lose = snapshot.child("lose").getValue(Long.class);
                        Long draw = snapshot.child("draw").getValue(Long.class);
                        Long score = snapshot.child("score").getValue(Long.class);

                        if (username == null) username = "Người chơi";
                        if (email == null) email = currentUser.getEmail();

                        if (win == null) win = 0L;
                        if (lose == null) lose = 0L;
                        if (draw == null) draw = 0L;
                        if (score == null) score = 0L;

                        tvUserInfo.setText("Xin chào, " + username + "\n" + email);
                        tvStats.setText("Thắng: " + win + " | Thua: " + lose + " | Hòa: " + draw + " | Điểm: " + score);
                    } else {
                        tvUserInfo.setText("Xin chào\n" + currentUser.getEmail());
                        tvStats.setText("Chưa có thống kê");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupEvents() {
        btnPlayAI.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Chơi với AI sẽ làm ở bước sau", Toast.LENGTH_SHORT).show();
        });

        btnPlayOnline.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Online sẽ làm sau", Toast.LENGTH_SHORT).show();
        });

        btnHistory.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Lịch sử sẽ làm sau", Toast.LENGTH_SHORT).show();
        });

        btnRanking.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng Bảng xếp hạng sẽ làm sau", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            goToLogin();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}