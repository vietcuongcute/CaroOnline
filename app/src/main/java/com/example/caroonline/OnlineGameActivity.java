package com.example.caroonline;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.HashMap;
import java.util.Map;

public class OnlineGameActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 15;
    private static final int WIN_COUNT = 5;

    private TextView tvRoomInfo, tvPlayers, tvTurn;
    private GridLayout gridBoard;
    private Button btnLeaveRoom;

    private Button[][] buttons;
    private String[][] board;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference roomRef;
    private DatabaseReference usersRef;
    private DatabaseReference matchesRef;

    private ValueEventListener roomListener;

    private String roomCode;
    private String mySymbol;
    private String myUid;

    private String playerXId = "";
    private String playerOId = "";
    private String playerXName = "";
    private String playerOName = "";
    private String currentTurn = "X";
    private String status = "waiting";
    private String winner = "";


    private boolean resultHandled = false;

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        roomCode = getIntent().getStringExtra("roomCode");
        mySymbol = getIntent().getStringExtra("mySymbol");

        if (roomCode == null || mySymbol == null) {
            Toast.makeText(this, "Thiếu thông tin phòng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        myUid = currentUser.getUid();

        roomRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms").child(roomCode);
        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");
        matchesRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("matches");

        initViews();

        buttons = new Button[BOARD_SIZE][BOARD_SIZE];
        board = new String[BOARD_SIZE][BOARD_SIZE];

        createBoard();
        listenRoomChanges();

        btnLeaveRoom.setOnClickListener(v -> confirmLeaveRoom());
    }

    private void initViews() {
        tvRoomInfo = findViewById(R.id.tvRoomInfo);
        tvPlayers = findViewById(R.id.tvPlayers);
        tvTurn = findViewById(R.id.tvTurn);
        gridBoard = findViewById(R.id.gridBoard);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);
    }

    private void createBoard() {
        gridBoard.removeAllViews();
        gridBoard.setColumnCount(BOARD_SIZE);
        gridBoard.setRowCount(BOARD_SIZE);

        int cellSize = dpToPx(38);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Button cell = new Button(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(params);

                cell.setText("");
                cell.setTextSize(14);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(0, 0, 0, 0);
                cell.setBackgroundColor(Color.WHITE);

                final int r = row;
                final int c = col;

                cell.setOnClickListener(v -> handleCellClick(r, c));

                buttons[row][col] = cell;
                board[row][col] = "";
                gridBoard.addView(cell);
            }
        }
    }

    private void listenRoomChanges() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(OnlineGameActivity.this, "Phòng không còn tồn tại", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                loadRoomInfo(snapshot);
                loadBoard(snapshot);
                updateUI();

                if (status.equals("finished") && !resultHandled) {
                    resultHandled = true;
                    handleGameFinished();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(OnlineGameActivity.this, "Lỗi realtime: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        roomRef.addValueEventListener(roomListener);
    }

    private void loadRoomInfo(DataSnapshot snapshot) {
        String xId = snapshot.child("playerXId").getValue(String.class);
        String oId = snapshot.child("playerOId").getValue(String.class);
        String xName = snapshot.child("playerXName").getValue(String.class);
        String oName = snapshot.child("playerOName").getValue(String.class);
        String turn = snapshot.child("currentTurn").getValue(String.class);
        String roomStatus = snapshot.child("status").getValue(String.class);
        String roomWinner = snapshot.child("winner").getValue(String.class);

        playerXId = xId == null ? "" : xId;
        playerOId = oId == null ? "" : oId;
        playerXName = xName == null ? "" : xName;
        playerOName = oName == null ? "" : oName;
        currentTurn = turn == null ? "X" : turn;
        status = roomStatus == null ? "waiting" : roomStatus;
        winner = roomWinner == null ? "" : roomWinner;
    }

    private void loadBoard(DataSnapshot snapshot) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                board[row][col] = "";
                buttons[row][col].setText("");
                buttons[row][col].setEnabled(true);
                buttons[row][col].setTypeface(null, Typeface.NORMAL);
            }
        }

        DataSnapshot boardSnapshot = snapshot.child("board");

        for (DataSnapshot moveSnapshot : boardSnapshot.getChildren()) {
            String key = moveSnapshot.getKey();
            String value = moveSnapshot.getValue(String.class);

            if (key == null || value == null) continue;

            String[] parts = key.split("_");

            if (parts.length != 2) continue;

            try {
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);

                if (isInsideBoard(row, col)) {
                    board[row][col] = value;
                    buttons[row][col].setText(value);
                    buttons[row][col].setEnabled(false);
                    buttons[row][col].setTypeface(null, Typeface.BOLD);

                    if (value.equals("X")) {
                        buttons[row][col].setTextColor(Color.BLUE);
                    } else {
                        buttons[row][col].setTextColor(Color.RED);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void updateUI() {
        tvRoomInfo.setText("Phòng: " + roomCode + " | Bạn là: " + mySymbol);

        String oName = playerOName.isEmpty() ? "Đang chờ..." : playerOName;
        tvPlayers.setText("X: " + playerXName + " | O: " + oName);

        if (status.equals("waiting")) {
            tvTurn.setText("Đang chờ người chơi thứ 2...");
            setBoardEnabled(false);
            return;
        }

        if (status.equals("finished")) {
            setBoardEnabled(false);

            if (winner.equals("DRAW")) {
                tvTurn.setText("Trận đấu hòa");
            } else {
                tvTurn.setText("Người thắng: " + getWinnerName());
            }
            return;
        }

        if (currentTurn.equals(mySymbol)) {
            tvTurn.setText("Đến lượt bạn: " + mySymbol);
            setBoardEnabled(true);
        } else {
            tvTurn.setText("Đang chờ đối thủ đánh...");
            setBoardEnabled(false);
        }
    }

    private void setBoardEnabled(boolean enabled) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col].equals("")) {
                    buttons[row][col].setEnabled(enabled);
                } else {
                    buttons[row][col].setEnabled(false);
                }
            }
        }
    }

    private void handleCellClick(int row, int col) {
        if (!status.equals("playing")) {
            Toast.makeText(this, "Trận đấu chưa bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentTurn.equals(mySymbol)) {
            Toast.makeText(this, "Chưa tới lượt bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!board[row][col].equals("")) {
            return;
        }

        board[row][col] = mySymbol;

        String key = row + "_" + col;

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("board/" + key, mySymbol);

        if (checkWin(row, col, mySymbol)) {
            updateMap.put("status", "finished");
            updateMap.put("winner", mySymbol);
            updateMap.put("finishedAt", System.currentTimeMillis());
        } else if (isBoardFull()) {
            updateMap.put("status", "finished");
            updateMap.put("winner", "DRAW");
            updateMap.put("finishedAt", System.currentTimeMillis());
        } else {
            updateMap.put("currentTurn", mySymbol.equals("X") ? "O" : "X");
        }

        roomRef.updateChildren(updateMap);
    }

    private void handleGameFinished() {
        String message;

        if (winner.equals("DRAW")) {
            message = "Trận đấu hòa!";
            updateOnlineStatsForBoth("DRAW");
            saveOnlineMatchHistory("DRAW");
        } else if (winner.equals(mySymbol)) {
            message = "Bạn thắng!";
            updateOnlineStatsForBoth(winner);
            saveOnlineMatchHistory(winner);
        } else {
            message = "Bạn thua!";
        }

        new AlertDialog.Builder(this)
                .setTitle("Kết quả")
                .setMessage(message)
                .setPositiveButton("Về trang chủ", (dialog, which) -> deleteRoomAndFinish())
                .setCancelable(false)
                .show();
    }

    private void updateOnlineStatsForBoth(String winnerSymbol) {
        if (!mySymbol.equals("X")) {
            return;
        }

        if (winnerSymbol.equals("DRAW")) {
            updateOnlineStats(playerXId, "DRAW");
            updateOnlineStats(playerOId, "DRAW");
        } else if (winnerSymbol.equals("X")) {
            updateOnlineStats(playerXId, "WIN");
            updateOnlineStats(playerOId, "LOSE");
        } else if (winnerSymbol.equals("O")) {
            updateOnlineStats(playerXId, "LOSE");
            updateOnlineStats(playerOId, "WIN");
        }
    }

    private void updateOnlineStats(String uid, String result) {
        if (uid == null || uid.isEmpty()) {
            return;
        }

        usersRef.child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    Long onlineTotalMatches = snapshot.child("onlineTotalMatches").getValue(Long.class);
                    Long onlineWin = snapshot.child("onlineWin").getValue(Long.class);
                    Long onlineLose = snapshot.child("onlineLose").getValue(Long.class);
                    Long onlineDraw = snapshot.child("onlineDraw").getValue(Long.class);
                    Long onlineScore = snapshot.child("onlineScore").getValue(Long.class);

                    if (onlineTotalMatches == null) onlineTotalMatches = 0L;
                    if (onlineWin == null) onlineWin = 0L;
                    if (onlineLose == null) onlineLose = 0L;
                    if (onlineDraw == null) onlineDraw = 0L;
                    if (onlineScore == null) onlineScore = 0L;

                    onlineTotalMatches++;

                    if (result.equals("WIN")) {
                        onlineWin++;
                        onlineScore += 3;
                    } else if (result.equals("LOSE")) {
                        onlineLose++;
                        onlineScore -= 1;
                    } else if (result.equals("DRAW")) {
                        onlineDraw++;
                        onlineScore += 1;
                    }

                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("onlineTotalMatches", onlineTotalMatches);
                    updateMap.put("onlineWin", onlineWin);
                    updateMap.put("onlineLose", onlineLose);
                    updateMap.put("onlineDraw", onlineDraw);
                    updateMap.put("onlineScore", onlineScore);

                    usersRef.child(uid).updateChildren(updateMap);
                });
    }

    private void saveOnlineMatchHistory(String winnerSymbol) {
        if (!mySymbol.equals("X")) {
            return;
        }

        String matchId = matchesRef.push().getKey();

        if (matchId == null) {
            return;
        }

        String resultText;
        String winnerName;

        if (winnerSymbol.equals("DRAW")) {
            resultText = "DRAW";
            winnerName = "Hòa";
        } else if (winnerSymbol.equals("X")) {
            resultText = "X_WIN";
            winnerName = playerXName;
        } else {
            resultText = "O_WIN";
            winnerName = playerOName;
        }

        HashMap<String, Object> matchMap = new HashMap<>();
        matchMap.put("matchId", matchId);
        matchMap.put("roomCode", roomCode);
        matchMap.put("mode", "ONLINE");
        matchMap.put("playerXId", playerXId);
        matchMap.put("playerXName", playerXName);
        matchMap.put("playerOId", playerOId);
        matchMap.put("playerOName", playerOName);
        matchMap.put("winner", winnerName);
        matchMap.put("result", resultText);
        matchMap.put("totalMoves", countTotalMoves());
        matchMap.put("createdAt", System.currentTimeMillis());

        matchesRef.child(matchId).setValue(matchMap);
    }

    private int countTotalMoves() {
        int count = 0;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!board[row][col].equals("")) {
                    count++;
                }
            }
        }

        return count;
    }

    private String getWinnerName() {
        if (winner.equals("X")) {
            return playerXName;
        } else if (winner.equals("O")) {
            return playerOName;
        } else {
            return "Hòa";
        }
    }

    private boolean checkWin(int row, int col, String symbol) {
        return countConsecutive(row, col, 0, 1, symbol) == WIN_COUNT
                || countConsecutive(row, col, 1, 0, symbol) == WIN_COUNT
                || countConsecutive(row, col, 1, 1, symbol) == WIN_COUNT
                || countConsecutive(row, col, 1, -1, symbol) == WIN_COUNT;
    }

    private int countConsecutive(int row, int col, int dRow, int dCol, String symbol) {
        int count = 1;

        int r = row + dRow;
        int c = col + dCol;

        while (isInsideBoard(r, c) && board[r][c].equals(symbol)) {
            count++;
            r += dRow;
            c += dCol;
        }

        r = row - dRow;
        c = col - dCol;

        while (isInsideBoard(r, c) && board[r][c].equals(symbol)) {
            count++;
            r -= dRow;
            c -= dCol;
        }

        return count;
    }

    private boolean isBoardFull() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col].equals("")) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    // Xác nhận rời phòng
// Khi người chơi rời phòng thì xóa luôn phòng trên Firebase
    private void confirmLeaveRoom() {
        new AlertDialog.Builder(this)
                .setTitle("Rời phòng")
                .setMessage("Bạn có chắc muốn rời phòng? Phòng sẽ bị xóa.")
                .setPositiveButton("Rời", (dialog, which) -> leaveAndDeleteRoom())
                .setNegativeButton("Ở lại", null)
                .show();
    }
    // Hàm xóa phòng khỏi Firebase rồi thoát màn hình
    private void leaveAndDeleteRoom() {
        if (roomRef == null) {
            finish();
            return;
        }

        roomRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã rời và xóa phòng", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi xóa phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    // Hàm xóa phòng sau khi trận kết thúc rồi quay về trang chủ
    private void deleteRoomAndFinish() {
        if (roomRef == null) {
            finish();
            return;
        }

        roomRef.removeValue()
                .addOnCompleteListener(task -> finish());
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomRef != null && roomListener != null) {
            roomRef.removeEventListener(roomListener);
        }
    }
}