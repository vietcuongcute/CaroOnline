package com.example.caroonline;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private TextView tvHistoryStatus;
    private LinearLayout layoutHistoryList;
    private Button btnBackHome;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference matchesRef;

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        matchesRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("matches");

        tvHistoryStatus = findViewById(R.id.tvHistoryStatus);
        layoutHistoryList = findViewById(R.id.layoutHistoryList);
        btnBackHome = findViewById(R.id.btnBackHome);

        btnBackHome.setOnClickListener(v -> finish());

        loadMatchHistory();
    }

    // Hàm tải lịch sử trận đấu của user hiện tại
    private void loadMatchHistory() {
        String uid = currentUser.getUid();

        tvHistoryStatus.setText("Đang tải lịch sử...");
        layoutHistoryList.removeAllViews();

        matchesRef.orderByChild("playerId").equalTo(uid).get()
                .addOnSuccessListener(snapshot -> {
                    layoutHistoryList.removeAllViews();

                    if (!snapshot.exists()) {
                        tvHistoryStatus.setText("Bạn chưa có trận đấu nào");
                        return;
                    }

                    tvHistoryStatus.setText("Danh sách trận đã chơi");

                    // Duyệt từng trận đấu trong node matches
                    for (com.google.firebase.database.DataSnapshot matchSnapshot : snapshot.getChildren()) {
                        String mode = matchSnapshot.child("mode").getValue(String.class);
                        String result = matchSnapshot.child("result").getValue(String.class);
                        String winner = matchSnapshot.child("winner").getValue(String.class);
                        Long totalMoves = matchSnapshot.child("totalMoves").getValue(Long.class);
                        Long createdAt = matchSnapshot.child("createdAt").getValue(Long.class);

                        if (mode == null) mode = "AI";
                        if (result == null) result = "UNKNOWN";
                        if (winner == null) winner = "UNKNOWN";
                        if (totalMoves == null) totalMoves = 0L;
                        if (createdAt == null) createdAt = 0L;

                        addMatchItem(mode, result, winner, totalMoves, createdAt);
                    }
                })
                .addOnFailureListener(e -> {
                    tvHistoryStatus.setText("Lỗi tải lịch sử");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Hàm thêm một item trận đấu vào giao diện
    private void addMatchItem(String mode, String result, String winner, Long totalMoves, Long createdAt) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        itemLayout.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, dpToPx(10));
        itemLayout.setLayoutParams(itemParams);

        TextView tvResult = new TextView(this);
        tvResult.setTextSize(18);
        tvResult.setTextColor(getResultColor(result));
        tvResult.setTypeface(null, android.graphics.Typeface.BOLD);
        tvResult.setText(getResultText(result));

        TextView tvInfo = new TextView(this);
        tvInfo.setTextSize(15);
        tvInfo.setTextColor(Color.DKGRAY);
        tvInfo.setPadding(0, dpToPx(6), 0, 0);

        String dateText = formatTime(createdAt);

        tvInfo.setText(
                "Chế độ: " + mode +
                        "\nNgười thắng: " + winner +
                        "\nTổng số nước: " + totalMoves +
                        "\nThời gian: " + dateText
        );

        itemLayout.addView(tvResult);
        itemLayout.addView(tvInfo);

        // Thêm item mới lên đầu danh sách để trận mới dễ thấy hơn
        layoutHistoryList.addView(itemLayout, 0);
    }

    // Đổi mã kết quả sang chữ dễ hiểu
    private String getResultText(String result) {
        if (result.equals("WIN")) {
            return "Kết quả: Thắng";
        } else if (result.equals("LOSE")) {
            return "Kết quả: Thua";
        } else if (result.equals("DRAW")) {
            return "Kết quả: Hòa";
        } else {
            return "Kết quả: Không xác định";
        }
    }

    // Đổi màu theo kết quả
    private int getResultColor(String result) {
        if (result.equals("WIN")) {
            return Color.rgb(46, 125, 50);
        } else if (result.equals("LOSE")) {
            return Color.rgb(198, 40, 40);
        } else if (result.equals("DRAW")) {
            return Color.rgb(245, 124, 0);
        } else {
            return Color.DKGRAY;
        }
    }

    // Chuyển timestamp thành ngày giờ dễ đọc
    private String formatTime(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "Không rõ";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Hàm chuyển dp sang px
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}