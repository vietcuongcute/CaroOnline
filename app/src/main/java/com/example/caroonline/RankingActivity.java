package com.example.caroonline;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;

public class RankingActivity extends AppCompatActivity {

    private TextView tvRankingStatus;
    private LinearLayout layoutRankingList;
    private Button btnBackHome;

    private DatabaseReference usersRef;

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        tvRankingStatus = findViewById(R.id.tvRankingStatus);
        layoutRankingList = findViewById(R.id.layoutRankingList);
        btnBackHome = findViewById(R.id.btnBackHome);

        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");

        btnBackHome.setOnClickListener(v -> finish());

        loadRanking();
    }

    // Hàm tải danh sách người chơi từ Firebase
    private void loadRanking() {
        tvRankingStatus.setText("Tính điểm Online: Thắng +3, Hòa +1, Thua -1");
        layoutRankingList.removeAllViews();

        usersRef.get()
                .addOnSuccessListener(snapshot -> {
                    ArrayList<PlayerRank> playerList = new ArrayList<>();

                    for (com.google.firebase.database.DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String username = userSnapshot.child("username").getValue(String.class);
                        String email = userSnapshot.child("email").getValue(String.class);

                        Long win = userSnapshot.child("onlineWin").getValue(Long.class);
                        Long lose = userSnapshot.child("onlineLose").getValue(Long.class);
                        Long draw = userSnapshot.child("onlineDraw").getValue(Long.class);
                        Long score = userSnapshot.child("onlineScore").getValue(Long.class);
                        Long totalMatches = userSnapshot.child("onlineTotalMatches").getValue(Long.class);

                        if (username == null || username.isEmpty()) username = "Người chơi";
                        if (email == null) email = "";
                        if (win == null) win = 0L;
                        if (lose == null) lose = 0L;
                        if (draw == null) draw = 0L;
                        if (score == null) score = 0L;
                        if (totalMatches == null) totalMatches = 0L;

                        playerList.add(new PlayerRank(username, email, win, lose, draw, score, totalMatches));
                    }

                    if (playerList.isEmpty()) {
                        tvRankingStatus.setText("Chưa có người chơi nào");
                        return;
                    }

                    // Sắp xếp theo điểm giảm dần.
                    // Nếu bằng điểm thì ưu tiên người có nhiều trận thắng hơn.
                    Collections.sort(playerList, (p1, p2) -> {
                        int scoreCompare = Long.compare(p2.score, p1.score);

                        if (scoreCompare != 0) {
                            return scoreCompare;
                        }

                        return Long.compare(p2.win, p1.win);
                    });

                    tvRankingStatus.setText("Top người chơi có điểm cao nhất");

                    int rank = 1;

                    for (PlayerRank player : playerList) {
                        addRankingItem(rank, player);
                        rank++;
                    }
                })
                .addOnFailureListener(e -> {
                    tvRankingStatus.setText("Lỗi tải bảng xếp hạng");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Hàm thêm một người chơi vào giao diện bảng xếp hạng
    private void addRankingItem(int rank, PlayerRank player) {
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

        TextView tvName = new TextView(this);
        tvName.setTextSize(18);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(getRankColor(rank));

        tvName.setText(getRankIcon(rank) + " Hạng " + rank + ": " + player.username);

        TextView tvInfo = new TextView(this);
        tvInfo.setTextSize(15);
        tvInfo.setTextColor(Color.DKGRAY);
        tvInfo.setPadding(0, dpToPx(6), 0, 0);

        tvInfo.setText(
                "Email: " + player.email +
                        "\nĐiểm: " + player.score +
                        "\nTổng trận: " + player.totalMatches +
                        "\nThắng: " + player.win +
                        " | Thua: " + player.lose +
                        " | Hòa: " + player.draw
        );

        itemLayout.addView(tvName);
        itemLayout.addView(tvInfo);

        layoutRankingList.addView(itemLayout);
    }

    // Icon top 3 cho đẹp
    private String getRankIcon(int rank) {
        if (rank == 1) {
            return "🏆";
        } else if (rank == 2) {
            return "🥈";
        } else if (rank == 3) {
            return "🥉";
        } else {
            return "⭐";
        }
    }

    // Màu top 3
    private int getRankColor(int rank) {
        if (rank == 1) {
            return Color.rgb(245, 124, 0);
        } else if (rank == 2) {
            return Color.rgb(97, 97, 97);
        } else if (rank == 3) {
            return Color.rgb(121, 85, 72);
        } else {
            return Color.rgb(33, 33, 33);
        }
    }

    // Chuyển dp sang px
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // Class nhỏ để lưu thông tin xếp hạng
    private static class PlayerRank {
        String username;
        String email;
        long win;
        long lose;
        long draw;
        long score;
        long totalMatches;

        PlayerRank(String username, String email, long win, long lose, long draw, long score, long totalMatches) {
            this.username = username;
            this.email = email;
            this.win = win;
            this.lose = lose;
            this.draw = draw;
            this.score = score;
            this.totalMatches = totalMatches;
        }
    }
}