package com.example.caroonline;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import com.example.caroonline.ai.MinimaxAI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;



public class GameAIActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 15;
    private static final int WIN_COUNT = 5;

    private GridLayout gridBoard;
    private TextView tvTurn;
    private Button btnRestart;

    private Button[][] buttons;
    private String[][] board;
    private MinimaxAI minimaxAI;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference usersRef;
    private DatabaseReference matchesRef;

    private int totalMoves = 0;

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private boolean gameOver = false;
    private boolean playerTurn = true;

    private final String PLAYER = "X";
    private final String AI = "O";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_aiactivity);

        gridBoard = findViewById(R.id.gridBoard);
        tvTurn = findViewById(R.id.tvTurn);
        btnRestart = findViewById(R.id.btnRestart);

        buttons = new Button[BOARD_SIZE][BOARD_SIZE];
        board = new String[BOARD_SIZE][BOARD_SIZE];
        minimaxAI = new MinimaxAI();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");
        matchesRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("matches");

        createBoard();

        btnRestart.setOnClickListener(v -> resetGame());
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

                cell.setOnClickListener(v -> handlePlayerMove(r, c));

                buttons[row][col] = cell;
                board[row][col] = "";
                gridBoard.addView(cell);
            }
        }

        gameOver = false;
        playerTurn = true;
        totalMoves = 0;
        tvTurn.setText("Lượt của bạn: X");
    }

    private void handlePlayerMove(int row, int col) {
        if (gameOver || !playerTurn) {
            return;
        }

        if (!board[row][col].equals("")) {
            return;
        }

        makeMove(row, col, PLAYER);

        if (checkWin(row, col, PLAYER)) {
            gameOver = true;
            tvTurn.setText("Bạn thắng!");
            saveMatchResult("WIN");
            showResultDialog("Bạn thắng!");
            return;
        }

        if (isBoardFull()) {
            gameOver = true;
            tvTurn.setText("Hòa!");
            saveMatchResult("DRAW");
            showResultDialog("Trận đấu hòa!");
            return;
        }

        playerTurn = false;
        tvTurn.setText("AI đang đánh...");

        gridBoard.postDelayed(this::makeAIMove, 500);
    }
    private void makeAIMove() {
        if (gameOver) {
            return;
        }

        int[] move = minimaxAI.getBestMove(board);

        int row = move[0];
        int col = move[1];

        if (!isInsideBoard(row, col) || !board[row][col].equals("")) {
            playerTurn = true;
            tvTurn.setText("Lượt của bạn: X");
            return;
        }

        makeMove(row, col, AI);

        if (checkWin(row, col, AI)) {
            gameOver = true;
            tvTurn.setText("AI thắng!");
            saveMatchResult("LOSE");
            showResultDialog("AI thắng!");
            return;
        }

        if (isBoardFull()) {
            gameOver = true;
            tvTurn.setText("Hòa!");
            saveMatchResult("DRAW");
            showResultDialog("Trận đấu hòa!");
            return;
        }

        playerTurn = true;
        tvTurn.setText("Lượt của bạn: X");
    }

    private void makeMove(int row, int col, String symbol) {
        board[row][col] = symbol;
        buttons[row][col].setText(symbol);
        buttons[row][col].setEnabled(false);
        totalMoves++;

        if (symbol.equals(PLAYER)) {
            buttons[row][col].setTextColor(Color.BLUE);
        } else {
            buttons[row][col].setTextColor(Color.RED);
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

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
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

    private void showResultDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Kết quả")
                .setMessage(message)
                .setPositiveButton("Chơi lại", (dialog, which) -> resetGame())
                .setNegativeButton("Về trang chủ", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void resetGame() {
        createBoard();
    }

    private void saveMatchResult(String result) {
        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        String matchId = matchesRef.push().getKey();

        if (matchId == null) {
            return;
        }

        HashMap<String, Object> matchMap = new HashMap<>();
        matchMap.put("matchId", matchId);
        matchMap.put("playerId", uid);
        matchMap.put("playerEmail", email);
        matchMap.put("mode", "AI");
        matchMap.put("result", result);
        matchMap.put("totalMoves", totalMoves);
        matchMap.put("createdAt", System.currentTimeMillis());

        usersRef.child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.child("username").getValue(String.class);

                    if (username == null || username.isEmpty()) {
                        username = email;
                    }

                    if (result.equals("WIN")) {
                        matchMap.put("winner", username);
                    } else if (result.equals("LOSE")) {
                        matchMap.put("winner", "AI");
                    } else {
                        matchMap.put("winner", "Hòa");
                    }

                    matchesRef.child(matchId).setValue(matchMap);

                    // Cập nhật thống kê user
                    updateUserStats(uid, result);
                })
                .addOnFailureListener(e -> {
                    if (result.equals("WIN")) {
                        matchMap.put("winner", email);
                    } else if (result.equals("LOSE")) {
                        matchMap.put("winner", "AI");
                    } else {
                        matchMap.put("winner", "Hòa");
                    }

                    matchesRef.child(matchId).setValue(matchMap);

                    // Cập nhật thống kê user
                    updateUserStats(uid, result);
                });
    }

    private void updateUserStats(String uid, String result) {
        usersRef.child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }

                    Long aiTotalMatches = snapshot.child("aiTotalMatches").getValue(Long.class);
                    Long aiWin = snapshot.child("aiWin").getValue(Long.class);
                    Long aiLose = snapshot.child("aiLose").getValue(Long.class);
                    Long aiDraw = snapshot.child("aiDraw").getValue(Long.class);

                    if (aiTotalMatches == null) aiTotalMatches = 0L;
                    if (aiWin == null) aiWin = 0L;
                    if (aiLose == null) aiLose = 0L;
                    if (aiDraw == null) aiDraw = 0L;

                    aiTotalMatches++;

                    if (result.equals("WIN")) {
                        aiWin++;
                    } else if (result.equals("LOSE")) {
                        aiLose++;
                    } else if (result.equals("DRAW")) {
                        aiDraw++;
                    }

                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("aiTotalMatches", aiTotalMatches);
                    updateMap.put("aiWin", aiWin);
                    updateMap.put("aiLose", aiLose);
                    updateMap.put("aiDraw", aiDraw);

                    usersRef.child(uid).updateChildren(updateMap);
                });
    }


    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}