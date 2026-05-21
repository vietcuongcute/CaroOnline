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
    Button btnPlayAI, btnPlayOnline, btnHistory, btnRanking, btnAbout, btnLogout;


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

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUser != null && usersRef != null) {
            loadUserInfo();
        }
    }

    private void initViews() {
        tvUserInfo = findViewById(R.id.tvUserInfo);
        tvStats = findViewById(R.id.tvStats);
        btnAbout = findViewById(R.id.btnAbout);
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

                        Long aiWin = snapshot.child("aiWin").getValue(Long.class);
                        Long aiLose = snapshot.child("aiLose").getValue(Long.class);
                        Long aiDraw = snapshot.child("aiDraw").getValue(Long.class);
                        Long aiTotalMatches = snapshot.child("aiTotalMatches").getValue(Long.class);

                        Long onlineWin = snapshot.child("onlineWin").getValue(Long.class);
                        Long onlineLose = snapshot.child("onlineLose").getValue(Long.class);
                        Long onlineDraw = snapshot.child("onlineDraw").getValue(Long.class);
                        Long onlineTotalMatches = snapshot.child("onlineTotalMatches").getValue(Long.class);
                        Long onlineScore = snapshot.child("onlineScore").getValue(Long.class);

                        if (aiWin == null) aiWin = 0L;
                        if (aiLose == null) aiLose = 0L;
                        if (aiDraw == null) aiDraw = 0L;
                        if (aiTotalMatches == null) aiTotalMatches = 0L;

                        if (onlineWin == null) onlineWin = 0L;
                        if (onlineLose == null) onlineLose = 0L;
                        if (onlineDraw == null) onlineDraw = 0L;
                        if (onlineTotalMatches == null) onlineTotalMatches = 0L;
                        if (onlineScore == null) onlineScore = 0L;

                        tvUserInfo.setText("Xin chào, " + username + "\n" + email);
                        tvStats.setText(
                                "AI: " + aiWin + " thắng | " + aiLose + " thua | " + aiDraw + " hòa" +
                                        "\nOnline: " + onlineWin + " thắng | " + onlineLose + " thua | " + onlineDraw + " hòa" +
                                        "\nĐiểm xếp hạng: " + onlineScore
                        );
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
            Intent intent = new Intent(HomeActivity.this, GameAIActivity.class);
            startActivity(intent);
        });

        btnPlayOnline.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, OnlineLobbyActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnRanking.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, RankingActivity.class);
            startActivity(intent);
        });
        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
            startActivity(intent);
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