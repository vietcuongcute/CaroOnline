package com.example.caroonline.ai;

import java.util.ArrayList;
import java.util.List;

public class MinimaxAI {

    // Kích thước bàn cờ
    private static final int BOARD_SIZE = 15;

    // Số quân liên tiếp để thắng
    private static final int WIN_COUNT = 5;

    // Giới hạn độ sâu Minimax.
    // Depth = 2 chạy khá ổn cho đồ án.
    private static final int MAX_DEPTH = 2;

    // Ký hiệu của AI và người chơi
    private final String AI = "O";
    private final String PLAYER = "X";

    // Hàm chính để lấy nước đi tốt nhất cho AI
    public int[] getBestMove(String[][] board) {
        List<int[]> possibleMoves = getPossibleMoves(board);

        // Nếu bàn cờ chưa có nước nào, AI đánh giữa bàn
        if (possibleMoves.isEmpty()) {
            return new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2};
        }

        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = possibleMoves.get(0);

        // Duyệt qua các nước đi có thể
        for (int[] move : possibleMoves) {
            int row = move[0];
            int col = move[1];

            // Thử cho AI đánh vào ô này
            board[row][col] = AI;

            // Nếu nước này giúp AI thắng ngay thì chọn luôn
            if (checkWin(board, row, col, AI)) {
                board[row][col] = "";
                return new int[]{row, col};
            }

            // Gọi Minimax để đánh giá nước đi
            int score = minimax(board, MAX_DEPTH - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);

            // Hoàn tác nước đi thử
            board[row][col] = "";

            // Nếu điểm tốt hơn thì cập nhật nước đi tốt nhất
            if (score > bestScore) {
                bestScore = score;
                bestMove = new int[]{row, col};
            }
        }

        return bestMove;
    }

    // Thuật toán Minimax có Alpha-Beta pruning
    private int minimax(String[][] board, int depth, boolean isMaximizing, int alpha, int beta) {
        // Nếu đạt độ sâu giới hạn hoặc hết ô trống thì đánh giá bàn cờ
        if (depth == 0 || isBoardFull(board)) {
            return evaluateBoard(board);
        }

        List<int[]> possibleMoves = getPossibleMoves(board);

        if (isMaximizing) {
            // Lượt AI: cố gắng lấy điểm cao nhất
            int bestScore = Integer.MIN_VALUE;

            for (int[] move : possibleMoves) {
                int row = move[0];
                int col = move[1];

                board[row][col] = AI;

                if (checkWin(board, row, col, AI)) {
                    board[row][col] = "";
                    return 1000000;
                }

                int score = minimax(board, depth - 1, false, alpha, beta);

                board[row][col] = "";

                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);

                // Cắt tỉa Alpha-Beta
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        } else {
            // Lượt người chơi: giả sử người chơi sẽ chọn nước tốt nhất cho họ
            int bestScore = Integer.MAX_VALUE;

            for (int[] move : possibleMoves) {
                int row = move[0];
                int col = move[1];

                board[row][col] = PLAYER;

                if (checkWin(board, row, col, PLAYER)) {
                    board[row][col] = "";
                    return -1000000;
                }

                int score = minimax(board, depth - 1, true, alpha, beta);

                board[row][col] = "";

                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);

                // Cắt tỉa Alpha-Beta
                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        }
    }

    // Lấy danh sách các ô trống có thể đánh
    // Để tối ưu, chỉ xét các ô trống nằm gần quân đã đánh
    private List<int[]> getPossibleMoves(String[][] board) {
        List<int[]> moves = new ArrayList<>();

        boolean hasMove = false;

        // Kiểm tra bàn cờ đã có quân nào chưa
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!board[row][col].equals("")) {
                    hasMove = true;
                    break;
                }
            }
            if (hasMove) break;
        }

        // Nếu bàn cờ chưa có quân nào, trả về giữa bàn
        if (!hasMove) {
            moves.add(new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2});
            return moves;
        }

        // Chỉ xét các ô trống gần quân đã có trong phạm vi 2 ô
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col].equals("") && hasNeighbor(board, row, col, 2)) {
                    moves.add(new int[]{row, col});
                }
            }
        }

        return moves;
    }

    // Kiểm tra một ô trống có nằm gần quân nào đã đánh không
    private boolean hasNeighbor(String[][] board, int row, int col, int distance) {
        for (int r = row - distance; r <= row + distance; r++) {
            for (int c = col - distance; c <= col + distance; c++) {
                if (isInsideBoard(r, c) && !board[r][c].equals("")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Hàm đánh giá toàn bộ bàn cờ
    // Điểm dương có lợi cho AI, điểm âm có lợi cho người chơi
    private int evaluateBoard(String[][] board) {
        int aiScore = evaluatePlayer(board, AI);
        int playerScore = evaluatePlayer(board, PLAYER);

        return aiScore - playerScore;
    }

    // Đánh giá điểm của một bên trên toàn bộ bàn cờ
    private int evaluatePlayer(String[][] board, String symbol) {
        int score = 0;

        // Duyệt toàn bộ bàn cờ
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col].equals(symbol)) {
                    // Cộng điểm theo 4 hướng
                    score += evaluateDirection(board, row, col, 0, 1, symbol);   // ngang
                    score += evaluateDirection(board, row, col, 1, 0, symbol);   // dọc
                    score += evaluateDirection(board, row, col, 1, 1, symbol);   // chéo chính
                    score += evaluateDirection(board, row, col, 1, -1, symbol);  // chéo phụ
                }
            }
        }

        return score;
    }

    // Đánh giá một chuỗi quân theo một hướng
    private int evaluateDirection(String[][] board, int row, int col, int dRow, int dCol, String symbol) {
        int count = 0;
        int openEnds = 0;

        // Đếm số quân liên tiếp bắt đầu từ vị trí row, col theo hướng dRow, dCol
        int r = row;
        int c = col;

        while (isInsideBoard(r, c) && board[r][c].equals(symbol)) {
            count++;
            r += dRow;
            c += dCol;
        }

        // Kiểm tra đầu sau có bị chặn không
        if (isInsideBoard(r, c) && board[r][c].equals("")) {
            openEnds++;
        }

        // Kiểm tra đầu trước có bị chặn không
        int beforeRow = row - dRow;
        int beforeCol = col - dCol;

        if (isInsideBoard(beforeRow, beforeCol) && board[beforeRow][beforeCol].equals("")) {
            openEnds++;
        }

        return getScoreByCount(count, openEnds);
    }

    // Quy đổi chuỗi quân thành điểm
    private int getScoreByCount(int count, int openEnds) {
        if (openEnds == 0 && count < WIN_COUNT) {
            return 0;
        }

        if (count == 5) {
            return 100000;
        }

        // Vì luật của bạn là 6 quân không thắng, chuỗi lớn hơn 5 không cộng điểm thắng
        if (count > 5) {
            return 0;
        }

        if (count == 4) {
            if (openEnds == 2) return 10000;
            if (openEnds == 1) return 1000;
        }

        if (count == 3) {
            if (openEnds == 2) return 1000;
            if (openEnds == 1) return 100;
        }

        if (count == 2) {
            if (openEnds == 2) return 100;
            if (openEnds == 1) return 10;
        }

        if (count == 1) {
            return 1;
        }

        return 0;
    }

    // Kiểm tra thắng, chỉ đúng 5 quân liên tiếp mới thắng
    private boolean checkWin(String[][] board, int row, int col, String symbol) {
        return countConsecutive(board, row, col, 0, 1, symbol) == WIN_COUNT
                || countConsecutive(board, row, col, 1, 0, symbol) == WIN_COUNT
                || countConsecutive(board, row, col, 1, 1, symbol) == WIN_COUNT
                || countConsecutive(board, row, col, 1, -1, symbol) == WIN_COUNT;
    }

    // Đếm số quân liên tiếp theo một hướng
    private int countConsecutive(String[][] board, int row, int col, int dRow, int dCol, String symbol) {
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

    // Kiểm tra vị trí có nằm trong bàn cờ không
    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    // Kiểm tra bàn cờ đã đầy chưa
    private boolean isBoardFull(String[][] board) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col].equals("")) {
                    return false;
                }
            }
        }
        return true;
    }
}