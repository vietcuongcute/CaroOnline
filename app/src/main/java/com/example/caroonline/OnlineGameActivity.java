package com.example.caroonline;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.os.CountDownTimer;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

public class OnlineGameActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 15;
    private static final int WIN_COUNT = 5;

    private TextView tvRoomInfo, tvPlayers, tvTurn;
    private GridLayout gridBoard;
    private FrameLayout boardContainer;
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
    private int lastMoveRow = -1;
    private int lastMoveCol = -1;
    private TextView tvTimer;
    private CountDownTimer turnTimer;
    private static final long TURN_TIME_MS = 60000;
    private long turnStartedAt = 0;

    private WinningLineView winningLineView;
    private Handler resultHandler = new Handler(Looper.getMainLooper());

    private boolean resultHandled = false;
    private boolean playAgainDialogShown = false;
    private AlertDialog resultDialog;
    private AlertDialog playAgainDialog;


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
        roomRef.onDisconnect().removeValue();
        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");
        matchesRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("matches");

        initViews();

        buttons = new Button[BOARD_SIZE][BOARD_SIZE];
        board = new String[BOARD_SIZE][BOARD_SIZE];

        createBoard();
        listenRoomChanges();

        btnLeaveRoom.setOnClickListener(v -> confirmLeaveRoom());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeaveRoom();
            }
        });
    }

    private void initViews() {
        tvRoomInfo = findViewById(R.id.tvRoomInfo);
        tvPlayers = findViewById(R.id.tvPlayers);
        tvTurn = findViewById(R.id.tvTurn);
        gridBoard = findViewById(R.id.gridBoard);
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);
        tvTimer = findViewById(R.id.tvTimer);
        winningLineView = findViewById(R.id.winningLineView);
        boardContainer = findViewById(R.id.boardContainer);
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
        gridBoard.post(() -> {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    gridBoard.getWidth(),
                    gridBoard.getHeight()
            );

            winningLineView.setLayoutParams(params);
            winningLineView.bringToFront();
            winningLineView.setVisibility(View.VISIBLE);
        });
    }

    private void listenRoomChanges() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(OnlineGameActivity.this, "Đối thủ đã rời phòng", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                loadRoomInfo(snapshot);
                loadBoard(snapshot);
                updateUI();
                startTurnTimer();

                handlePlayAgainRequest(snapshot);
                handlePlayAgainResponse(snapshot);

                if (status.equals("playing")) {
                    resultHandled = false;
                    playAgainDialogShown = false;

                    if (resultDialog != null && resultDialog.isShowing()) {
                        resultDialog.dismiss();
                    }

                    if (playAgainDialog != null && playAgainDialog.isShowing()) {
                        playAgainDialog.dismiss();
                    }
                    resultHandler.removeCallbacksAndMessages(null);

                    if (winningLineView != null) {
                        winningLineView.clearLine();
                    }
                }

                if (status.equals("finished") && !resultHandled) {
                    resultHandled = true;
                    showWinningLineThenResult();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(OnlineGameActivity.this, "Lỗi realtime: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        roomRef.addValueEventListener(roomListener);
    }

    private void showWinningLineThenResult() {
        if (winner.equals("DRAW")) {
            handleGameFinished();
            return;
        }

        if (lastMoveRow == -1 || lastMoveCol == -1) {
            handleGameFinished();
            return;
        }

        boolean hasLine = drawWinningLine(lastMoveRow, lastMoveCol, winner);

        if (!hasLine) {
            handleGameFinished();
            return;
        }

        tvTurn.setText("Đã có người chiến thắng!");

        resultHandler.postDelayed(() -> {
            if (status.equals("finished")) {
                handleGameFinished();
            }
        }, 5000);
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
    private void handlePlayAgainResponse(DataSnapshot snapshot) {
        if (!mySymbol.equals("X")) {
            return;
        }

        Boolean playAgainRequest = snapshot.child("playAgainRequest").getValue(Boolean.class);
        String playAgainResponse = snapshot.child("playAgainResponse").getValue(String.class);

        if (playAgainRequest == null || !playAgainRequest) {
            return;
        }

        if (playAgainResponse == null || playAgainResponse.isEmpty()) {
            return;
        }

        if (playAgainResponse.equals("ACCEPT")) {
            playAgain();
        } else if (playAgainResponse.equals("REJECT")) {
            Toast.makeText(this, "Đối thủ đã từ chối chơi lại", Toast.LENGTH_LONG).show();
            deleteRoomAndFinish();
        }
    }
    private void handlePlayAgainRequest(DataSnapshot snapshot) {
        Boolean playAgainRequest = snapshot.child("playAgainRequest").getValue(Boolean.class);
        String playAgainRequester = snapshot.child("playAgainRequester").getValue(String.class);
        String playAgainResponse = snapshot.child("playAgainResponse").getValue(String.class);

        if (playAgainRequest == null || !playAgainRequest) {
            return;
        }

        if (playAgainRequester == null || playAgainRequester.equals(myUid)) {
            return;
        }

        if (playAgainResponse == null) {
            playAgainResponse = "";
        }

        if (!playAgainResponse.isEmpty()) {
            return;
        }

        if (playAgainDialogShown) {
            return;
        }

        playAgainDialogShown = true;

        if (playAgainDialog != null && playAgainDialog.isShowing()) {
            return;
        }

        playAgainDialog = new AlertDialog.Builder(this)
                .setTitle("Chơi lại")
                .setMessage("Đối thủ muốn chơi lại.\nBạn có đồng ý không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> acceptPlayAgain())
                .setNegativeButton("Từ chối", (dialog, which) -> rejectPlayAgain())
                .setCancelable(false)
                .create();

        playAgainDialog.show();
    }
    private void acceptPlayAgain() {
        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }

        if (playAgainDialog != null && playAgainDialog.isShowing()) {
            playAgainDialog.dismiss();
        }

        roomRef.child("playAgainResponse").setValue("ACCEPT")
                .addOnSuccessListener(unused -> {
                    if (!mySymbol.equals("X")) {
                        Toast.makeText(this, "Bạn đã đồng ý chơi lại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectPlayAgain() {
        roomRef.child("playAgainResponse").setValue("REJECT")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Bạn đã từ chối chơi lại", Toast.LENGTH_SHORT).show();
                    deleteRoomAndFinish();
                });
    }
    private void loadRoomInfo(DataSnapshot snapshot) {
        String xId = snapshot.child("playerXId").getValue(String.class);
        String oId = snapshot.child("playerOId").getValue(String.class);
        String xName = snapshot.child("playerXName").getValue(String.class);
        String oName = snapshot.child("playerOName").getValue(String.class);
        String turn = snapshot.child("currentTurn").getValue(String.class);
        String roomStatus = snapshot.child("status").getValue(String.class);
        String roomWinner = snapshot.child("winner").getValue(String.class);
        Long lastRow = snapshot.child("lastMoveRow").getValue(Long.class);
        Long lastCol = snapshot.child("lastMoveCol").getValue(Long.class);
        Long startedAt = snapshot.child("turnStartedAt").getValue(Long.class);

        playerXId = xId == null ? "" : xId;
        playerOId = oId == null ? "" : oId;
        playerXName = xName == null ? "" : xName;
        playerOName = oName == null ? "" : oName;
        currentTurn = turn == null ? "X" : turn;
        status = roomStatus == null ? "waiting" : roomStatus;
        winner = roomWinner == null ? "" : roomWinner;
        lastMoveRow = lastRow == null ? -1 : lastRow.intValue();
        lastMoveCol = lastCol == null ? -1 : lastCol.intValue();
        turnStartedAt = startedAt == null ? System.currentTimeMillis() : startedAt;
    }

    private void loadBoard(DataSnapshot snapshot) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                board[row][col] = "";
                buttons[row][col].setText("");
                buttons[row][col].setEnabled(true);
                buttons[row][col].setTypeface(null, Typeface.NORMAL);
                buttons[row][col].setBackgroundColor(Color.WHITE);
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
                    if (row == lastMoveRow && col == lastMoveCol) {
                        buttons[row][col].setBackgroundColor(Color.rgb(255, 245, 157));
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
        updateMap.put("lastMoveRow", row);
        updateMap.put("lastMoveCol", col);
        updateMap.put("turnStartedAt", System.currentTimeMillis());

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
        } else if (winner.equals(mySymbol)) {
            message = "Bạn thắng!";
        } else {
            message = "Bạn thua!";
        }

        // Chỉ máy X xử lý kết quả, và chỉ xử lý 1 lần
        if (mySymbol.equals("X")) {
            processOnlineResultOnce();
        }

        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }

        resultDialog = new AlertDialog.Builder(this)
                .setTitle("Kết quả")
                .setMessage(message)
                .setPositiveButton("Chơi lại", (dialog, which) -> requestPlayAgain())
                .setNegativeButton("Về trang chủ", (dialog, which) -> deleteRoomAndFinish())
                .setCancelable(false)
                .create();

        resultDialog.show();
    }
    private void playAgain() {
        if (!mySymbol.equals("X")) {
            Toast.makeText(this, "Chỉ chủ phòng mới được tạo ván mới", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("board", null);
        updateMap.put("currentTurn", "X");
        updateMap.put("status", "playing");
        updateMap.put("winner", "");
        updateMap.put("finishedAt", null);
        updateMap.put("resultSaved", false);
        updateMap.put("createdAt", System.currentTimeMillis());
        updateMap.put("lastMoveRow", null);
        updateMap.put("lastMoveCol", null);
        updateMap.put("playAgainRequest", false);
        updateMap.put("playAgainRequester", "");
        updateMap.put("playAgainResponse", "");
        updateMap.put("playAgainRequestedAt", null);

        updateMap.put("turnStartedAt", System.currentTimeMillis());

        roomRef.updateChildren(updateMap)
                .addOnSuccessListener(unused -> {
                    resultHandled = false;
                    playAgainDialogShown = false;
                    Toast.makeText(this, "Đã bắt đầu ván mới", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi chơi lại: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
    private void requestPlayAgain() {
        if (!mySymbol.equals("X")) {
            Toast.makeText(this, "Chỉ chủ phòng mới được gửi yêu cầu chơi lại", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("playAgainRequest", true);
        updateMap.put("playAgainRequester", myUid);
        updateMap.put("playAgainResponse", "");
        updateMap.put("playAgainRequestedAt", System.currentTimeMillis());

        roomRef.updateChildren(updateMap)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Đã gửi yêu cầu chơi lại, đang chờ đối thủ...", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
    private void processOnlineResultOnce() {
        roomRef.child("resultSaved").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean resultSaved = currentData.getValue(Boolean.class);

                if (resultSaved != null && resultSaved) {
                    return Transaction.abort();
                }

                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(OnlineGameActivity.this,
                            "Lỗi lưu kết quả: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (committed) {
                    updateOnlineStatsForBoth(winner);
                    saveOnlineMatchHistory(winner);
                }
            }
        });
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

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        if (!status.equals("playing")) {
            tvTimer.setText("Thời gian: --");
            return;
        }

        long elapsed = System.currentTimeMillis() - turnStartedAt;
        long remaining = TURN_TIME_MS - elapsed;

        if (remaining <= 0) {
            tvTimer.setText("Hết giờ!");

            if (currentTurn.equals(mySymbol)) {
                handleTimeoutLose();
            }
            return;
        }

        turnTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvTimer.setText("Thời gian: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Hết giờ!");

                if (currentTurn.equals(mySymbol) && status.equals("playing")) {
                    handleTimeoutLose();
                }
            }
        };

        turnTimer.start();
    }
    private void handleTimeoutLose() {
        String timeoutWinner = mySymbol.equals("X") ? "O" : "X";

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("status", "finished");
        updateMap.put("winner", timeoutWinner);
        updateMap.put("finishedAt", System.currentTimeMillis());
        updateMap.put("timeoutLoser", mySymbol);

        roomRef.updateChildren(updateMap);
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
        resultHandler.removeCallbacksAndMessages(null);
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        if (roomRef != null && roomListener != null) {
            roomRef.removeEventListener(roomListener);
        }

    }
}