package com.example.caroonline;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
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
    private static final long RESULT_DELAY_MS = 5000;

    private GridLayout gridBoard;
    private FrameLayout boardContainer;
    private WinningLineView winningLineView;
    private TextView tvTurn;
    private Button btnRestart;

    private Button[][] buttons;
    private String[][] board;
    private MinimaxAI minimaxAI;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference usersRef;
    private DatabaseReference matchesRef;

    private Handler resultHandler = new Handler(Looper.getMainLooper());
    private AlertDialog resultDialog;

    private int totalMoves = 0;
    private boolean gameOver = false;
    private boolean playerTurn = true;

    private int lastAIRow = -1;
    private int lastAICol = -1;

    private final String PLAYER = "X";
    private final String AI = "O";

    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_aiactivity);

        initViews();

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

    private void initViews() {
        gridBoard = findViewById(R.id.gridBoard);
        boardContainer = findViewById(R.id.boardContainer);
        winningLineView = findViewById(R.id.winningLineView);
        tvTurn = findViewById(R.id.tvTurn);
        btnRestart = findViewById(R.id.btnRestart);
    }

    private void createBoard() {
        resultHandler.removeCallbacksAndMessages(null);

        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }

        if (winningLineView != null) {
            winningLineView.clearLine();
        }

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
        lastAIRow = -1;
        lastAICol = -1;

        tvTurn.setText("Lượt của bạn: X");

        setupWinningLineView();
    }

    private void setupWinningLineView() {
        if (winningLineView == null) {
            return;
        }

        gridBoard.post(() -> {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    gridBoard.getWidth(),
                    gridBoard.getHeight()
            );

            winningLineView.setLayoutParams(params);
            winningLineView.bringToFront();
            winningLineView.setVisibility(View.VISIBLE);
            winningLineView.clearLine();
        });
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
            playerTurn = false;
            setBoardEnabled(false);
            tvTurn.setText("Bạn thắng!");

            saveMatchResult("WIN");
            showWinningLineThenResult(row, col, PLAYER, "Bạn thắng!");
            return;
        }

        if (isBoardFull()) {
            gameOver = true;
            playerTurn = false;
            setBoardEnabled(false);
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
        highlightLastAIMove(row, col);

        if (checkWin(row, col, AI)) {
            gameOver = true;
            playerTurn = false;
            setBoardEnabled(false);
            tvTurn.setText("AI thắng!");

            saveMatchResult("LOSE");
            showWinningLineThenResult(row, col, AI, "AI thắng!");
            return;
        }

        if (isBoardFull()) {
            gameOver = true;
            playerTurn = false;
            setBoardEnabled(false);
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

    private void highlightLastAIMove(int row, int col) {
        if (lastAIRow != -1 && lastAICol != -1) {
            buttons[lastAIRow][lastAICol].setBackgroundColor(Color.WHITE);
        }

        buttons[row][col].setBackgroundColor(Color.rgb(255, 249, 196));

        lastAIRow = row;
        lastAICol = col;
    }

    private void showWinningLineThenResult(int row, int col, String symbol, String message) {
        boolean hasLine = drawWinningLine(row, col, symbol);

        if (!hasLine) {
            showResultDialog(message);
            return;
        }

        resultHandler.postDelayed(() -> {
            if (gameOver) {
                showResultDialog(message);
            }
        }, RESULT_DELAY_MS);
    }

    private boolean drawWinningLine(int row, int col, String symbol) {
        int[][] directions = {
                {0, 1},
                {1, 0},
                {1, 1},
                {1, -1}
        };

        for (int[] dir : directions) {
            int dRow = dir[0];
            int dCol = dir[1];

            int[] line = getExactFiveLine(row, col, dRow, dCol, symbol);

            if (line != null) {
                int startRow = line[0];
                int startCol = line[1];
                int endRow = line[2];
                int endCol = line[3];

                Button startButton = buttons[startRow][startCol];
                Button endButton = buttons[endRow][endCol];

                gridBoard.post(() -> {
                    if (winningLineView == null) {
                        return;
                    }

                    winningLineView.bringToFront();
                    winningLineView.setVisibility(View.VISIBLE);

                    float startX = startButton.getLeft() + startButton.getWidth() / 2f;
                    float startY = startButton.getTop() + startButton.getHeight() / 2f;

                    float endX = endButton.getLeft() + endButton.getWidth() / 2f;
                    float endY = endButton.getTop() + endButton.getHeight() / 2f;

                    winningLineView.showWinningLine(startX, startY, endX, endY);
                });

                return true;
            }
        }

        return false;
    }

    private int[] getExactFiveLine(int row, int col, int dRow, int dCol, String symbol) {
        int startRow = row;
        int startCol = col;
        int endRow = row;
        int endCol = col;
        int count = 1;

        int r = row + dRow;
        int c = col + dCol;

        while (isInsideBoard(r, c) && board[r][c].equals(symbol)) {
            endRow = r;
            endCol = c;
            count++;
            r += dRow;
            c += dCol;
        }

        r = row - dRow;
        c = col - dCol;

        while (isInsideBoard(r, c) && board[r][c].equals(symbol)) {
            startRow = r;
            startCol = c;
            count++;
            r -= dRow;
            c -= dCol;
        }

        if (count == WIN_COUNT) {
            return new int[]{startRow, startCol, endRow, endCol};
        }

        return null;
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
        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }

        resultDialog = new AlertDialog.Builder(this)
                .setTitle("Kết quả")
                .setMessage(message)
                .setPositiveButton("Chơi lại", (dialog, which) -> resetGame())
                .setNegativeButton("Về trang chủ", (dialog, which) -> finish())
                .setCancelable(false)
                .create();

        resultDialog.show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        resultHandler.removeCallbacksAndMessages(null);

        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }
    }
}