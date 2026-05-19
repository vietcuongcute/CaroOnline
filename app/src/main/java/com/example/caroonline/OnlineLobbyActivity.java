package com.example.caroonline;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Random;

public class OnlineLobbyActivity extends AppCompatActivity {

    private EditText edtRoomCode;
    private Button btnCreateRoom, btnJoinRoom, btnBackHome;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference roomsRef;
    private DatabaseReference usersRef;

    private ListView listRooms;
    private ArrayAdapter<String> roomAdapter;
    private ArrayList<String> roomDisplayList;
    private ArrayList<String> roomCodeList;
    private static final String DATABASE_URL = "https://caroonline-e650f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_lobby);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        roomsRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms");
        usersRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");
        listRooms = findViewById(R.id.listRooms);

        roomDisplayList = new ArrayList<>();
        roomCodeList = new ArrayList<>();

        roomAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                roomDisplayList
        );

        listRooms.setAdapter(roomAdapter);

        loadWaitingRooms();

        listRooms.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRoomCode = roomCodeList.get(position);
            joinRoomByCode(selectedRoomCode);
        });

        initViews();
        setupEvents();
    }

    private void joinRoomByCode(String roomCode) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference roomRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("rooms")
                .child(roomCode);

        roomRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "Phòng không tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            String status = snapshot.child("status").getValue(String.class);
            String playerXId = snapshot.child("playerXId").getValue(String.class);
            String playerOId = snapshot.child("playerOId").getValue(String.class);

            if (!"waiting".equals(status)) {
                Toast.makeText(this, "Phòng đã bắt đầu hoặc đã kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser.getUid().equals(playerXId)) {
                Toast.makeText(this, "Bạn không thể tham gia phòng của chính mình", Toast.LENGTH_SHORT).show();
                return;
            }

            if (playerOId != null && !playerOId.isEmpty()) {
                Toast.makeText(this, "Phòng đã có người tham gia", Toast.LENGTH_SHORT).show();
                return;
            }

            joinRoomAsPlayerO(roomCode);
        });
    }
    private void initViews() {
        edtRoomCode = findViewById(R.id.edtRoomCode);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnBackHome = findViewById(R.id.btnBackHome);

    }
    private void loadWaitingRooms() {
        FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("rooms")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        roomDisplayList.clear();
                        roomCodeList.clear();

                        int totalRooms = 0;
                        int waitingRooms = 0;

                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            totalRooms++;

                            String roomCode = roomSnapshot.getKey();
                            String status = roomSnapshot.child("status").getValue(String.class);
                            String playerXName = roomSnapshot.child("playerXName").getValue(String.class);
                            String playerOId = roomSnapshot.child("playerOId").getValue(String.class);

                            if (roomCode == null) {
                                continue;
                            }

                            if (status == null) {
                                status = "";
                            }

                            if (playerOId == null) {
                                playerOId = "";
                            }

                            if (playerXName == null || playerXName.trim().isEmpty()) {
                                playerXName = "Người chơi";
                            }

                            // Chỉ hiện phòng đang chờ và chưa có người O
                            if (status.trim().equals("waiting") && playerOId.trim().isEmpty()) {
                                waitingRooms++;

                                roomCodeList.add(roomCode);
                                roomDisplayList.add("Phòng " + roomCode + " - Chủ phòng: " + playerXName);
                            }
                        }

                        roomAdapter.notifyDataSetChanged();


                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(
                                OnlineLobbyActivity.this,
                                "Lỗi tải phòng: " + error.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void setupEvents() {
        btnCreateRoom.setOnClickListener(v -> createRoom());

        btnJoinRoom.setOnClickListener(v -> {
            String roomCode = edtRoomCode.getText().toString().trim();

            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã phòng", Toast.LENGTH_SHORT).show();
                return;
            }

            joinRoomByCode(roomCode);
        });

        btnBackHome.setOnClickListener(v -> finish());
    }

    // Tạo phòng mới với mã 6 chữ số
    private void createRoom() {
        btnCreateRoom.setEnabled(false);

        String roomCode = generateRoomCode();

        roomsRef.child(roomCode).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Nếu mã bị trùng thì tạo lại
                        btnCreateRoom.setEnabled(true);
                        createRoom();
                        return;
                    }

                    createRoomData(roomCode);
                })
                .addOnFailureListener(e -> {
                    btnCreateRoom.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Tạo dữ liệu phòng trên Firebase
    private void createRoomData(String roomCode) {
        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        usersRef.child(uid).get()
                .addOnSuccessListener(userSnapshot -> {
                    String username = userSnapshot.child("username").getValue(String.class);

                    if (username == null || username.isEmpty()) {
                        username = email;
                    }

                    HashMap<String, Object> roomMap = new HashMap<>();
                    roomMap.put("roomCode", roomCode);

                    // Người tạo phòng là X
                    roomMap.put("playerXId", uid);
                    roomMap.put("playerXName", username);

                    // Người thứ hai chưa có
                    roomMap.put("playerOId", "");
                    roomMap.put("playerOName", "");

                    // Trạng thái phòng
                    roomMap.put("currentTurn", "X");
                    roomMap.put("status", "waiting");
                    roomMap.put("winner", "");
                    roomMap.put("createdAt", System.currentTimeMillis());
                    roomMap.put("turnStartedAt", System.currentTimeMillis());

                    roomsRef.child(roomCode).setValue(roomMap)
                            .addOnSuccessListener(unused -> {
                                btnCreateRoom.setEnabled(true);
                                Toast.makeText(this, "Tạo phòng thành công: " + roomCode, Toast.LENGTH_SHORT).show();
                                goToOnlineGame(roomCode, "X");
                            })
                            .addOnFailureListener(e -> {
                                btnCreateRoom.setEnabled(true);
                                Toast.makeText(this, "Lỗi lưu phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnCreateRoom.setEnabled(true);
                    Toast.makeText(this, "Lỗi lấy user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Tham gia phòng đã có
    private void joinRoom(String roomCode) {
        btnJoinRoom.setEnabled(false);

        roomsRef.child(roomCode).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        btnJoinRoom.setEnabled(true);
                        Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String status = snapshot.child("status").getValue(String.class);
                    String playerXId = snapshot.child("playerXId").getValue(String.class);
                    String playerOId = snapshot.child("playerOId").getValue(String.class);

                    if (status == null) status = "";
                    if (playerXId == null) playerXId = "";
                    if (playerOId == null) playerOId = "";

                    if (status.equals("playing") || !playerOId.isEmpty()) {
                        btnJoinRoom.setEnabled(true);
                        Toast.makeText(this, "Phòng đã đủ người", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (playerXId.equals(currentUser.getUid())) {
                        btnJoinRoom.setEnabled(true);
                        Toast.makeText(this, "Bạn là chủ phòng này", Toast.LENGTH_SHORT).show();
                        goToOnlineGame(roomCode, "X");
                        return;
                    }

                    joinRoomAsPlayerO(roomCode);
                })
                .addOnFailureListener(e -> {
                    btnJoinRoom.setEnabled(true);
                    Toast.makeText(this, "Lỗi tham gia phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Người tham gia phòng là O
    private void joinRoomAsPlayerO(String roomCode) {
        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        usersRef.child(uid).get()
                .addOnSuccessListener(userSnapshot -> {
                    String username = userSnapshot.child("username").getValue(String.class);

                    if (username == null || username.isEmpty()) {
                        username = email;
                    }

                    HashMap<String, Object> updateMap = new HashMap<>();
                    updateMap.put("playerOId", uid);
                    updateMap.put("playerOName", username);
                    updateMap.put("status", "playing");
                    updateMap.put("currentTurn", "X");

                    roomsRef.child(roomCode).updateChildren(updateMap)
                            .addOnSuccessListener(unused -> {
                                btnJoinRoom.setEnabled(true);
                                Toast.makeText(this, "Tham gia phòng thành công", Toast.LENGTH_SHORT).show();
                                goToOnlineGame(roomCode, "O");
                            })
                            .addOnFailureListener(e -> {
                                btnJoinRoom.setEnabled(true);
                                Toast.makeText(this, "Lỗi cập nhật phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnJoinRoom.setEnabled(true);
                    Toast.makeText(this, "Lỗi lấy user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Sinh mã phòng 6 chữ số
    private String generateRoomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    // Chuyển sang màn hình game online
    private void goToOnlineGame(String roomCode, String mySymbol) {
        Intent intent = new Intent(OnlineLobbyActivity.this, OnlineGameActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("mySymbol", mySymbol);
        startActivity(intent);
    }
}