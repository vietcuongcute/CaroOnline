package com.example.caroonline.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MinimaxAI {

    private static final int BOARD_SIZE = 15;
    private static final int WIN_COUNT = 5;

    // Độ khó cao. Nếu máy ảo bị lag thì đổi về 3.
    private static final int MAX_DEPTH = 4;

    // Giới hạn số nước tốt nhất để tránh duyệt quá nhiều.
    private static final int MAX_CANDIDATE_MOVES = 14;

    private final String AI = "O";
    private final String PLAYER = "X";

    public int[] getBestMove(String[][] board) {
        normalizeBoard(board);

        List<int[]> possibleMoves = getPossibleMoves(board);

        if (possibleMoves.isEmpty()) {
            return new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2};
        }

        // 1. AI thắng ngay thì đánh.
        int[] aiWinMove = findWinningMove(board, AI);
        if (aiWinMove != null) {
            return aiWinMove;
        }

        // 2. Người chơi thắng ngay thì chặn.
        int[] playerWinMove = findWinningMove(board, PLAYER);
        if (playerWinMove != null) {
            return playerWinMove;
        }

        // 3. AI tạo bẫy kép: sau nước này có từ 2 nước thắng.
        int[] aiForkMove = findForkMove(board, AI);
        if (aiForkMove != null) {
            return aiForkMove;
        }

        // 4. Chặn bẫy kép của người chơi.
        int[] playerForkMove = findForkMove(board, PLAYER);
        if (playerForkMove != null) {
            return playerForkMove;
        }

        // 5. Sắp xếp nước tốt trước rồi minimax.
        possibleMoves = getOrderedMoves(board, possibleMoves);

        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = possibleMoves.get(0);

        for (int[] move : possibleMoves) {
            int row = move[0];
            int col = move[1];

            board[row][col] = AI;

            int score = minimax(
                    board,
                    MAX_DEPTH - 1,
                    false,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE
            );

            board[row][col] = "";

            if (score > bestScore) {
                bestScore = score;
                bestMove = new int[]{row, col};
            }
        }

        return bestMove;
    }

    private int minimax(String[][] board, int depth, boolean isMaximizing, int alpha, int beta) {
        List<int[]> possibleMoves = getOrderedMoves(board, getPossibleMoves(board));

        if (depth == 0 || possibleMoves.isEmpty() || isBoardFull(board)) {
            return evaluateBoard(board);
        }

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;

            for (int[] move : possibleMoves) {
                int row = move[0];
                int col = move[1];

                board[row][col] = AI;

                if (checkWin(board, row, col, AI)) {
                    board[row][col] = "";
                    return 100000000 + depth;
                }

                int score = minimax(board, depth - 1, false, alpha, beta);

                board[row][col] = "";

                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);

                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;

            for (int[] move : possibleMoves) {
                int row = move[0];
                int col = move[1];

                board[row][col] = PLAYER;

                if (checkWin(board, row, col, PLAYER)) {
                    board[row][col] = "";
                    return -100000000 - depth;
                }

                int score = minimax(board, depth - 1, true, alpha, beta);

                board[row][col] = "";

                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);

                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        }
    }

    private int[] findWinningMove(String[][] board, String symbol) {
        List<int[]> moves = getOrderedMoves(board, getPossibleMoves(board));

        for (int[] move : moves) {
            int row = move[0];
            int col = move[1];

            board[row][col] = symbol;

            boolean win = checkWin(board, row, col, symbol);

            board[row][col] = "";

            if (win) {
                return new int[]{row, col};
            }
        }

        return null;
    }

    private int[] findForkMove(String[][] board, String symbol) {
        List<int[]> moves = getOrderedMoves(board, getPossibleMoves(board));

        for (int[] move : moves) {
            int row = move[0];
            int col = move[1];

            board[row][col] = symbol;

            int winningMoves = countWinningMoves(board, symbol);

            board[row][col] = "";

            if (winningMoves >= 2) {
                return new int[]{row, col};
            }
        }

        return null;
    }

    private int countWinningMoves(String[][] board, String symbol) {
        int count = 0;

        List<int[]> moves = getPossibleMoves(board);

        for (int[] move : moves) {
            int row = move[0];
            int col = move[1];

            board[row][col] = symbol;

            if (checkWin(board, row, col, symbol)) {
                count++;
            }

            board[row][col] = "";

            if (count >= 2) {
                return count;
            }
        }

        return count;
    }

    private List<int[]> getOrderedMoves(String[][] board, List<int[]> moves) {
        List<int[]> orderedMoves = new ArrayList<>(moves);

        Collections.sort(orderedMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int score1 = evaluateMove(board, move1[0], move1[1]);
                int score2 = evaluateMove(board, move2[0], move2[1]);
                return Integer.compare(score2, score1);
            }
        });

        if (orderedMoves.size() > MAX_CANDIDATE_MOVES) {
            return new ArrayList<>(orderedMoves.subList(0, MAX_CANDIDATE_MOVES));
        }

        return orderedMoves;
    }

    private int evaluateMove(String[][] board, int row, int col) {
        if (!isEmpty(board[row][col])) {
            return -1;
        }

        int score = 0;

        int center = BOARD_SIZE / 2;
        int distanceToCenter = Math.abs(row - center) + Math.abs(col - center);
        score += 50 - distanceToCenter;

        board[row][col] = AI;
        if (checkWin(board, row, col, AI)) {
            board[row][col] = "";
            return 100000000;
        }
        score += evaluatePoint(board, row, col, AI) * 2;
        board[row][col] = "";

        board[row][col] = PLAYER;
        if (checkWin(board, row, col, PLAYER)) {
            board[row][col] = "";
            return 90000000;
        }
        score += evaluatePoint(board, row, col, PLAYER) * 3;
        board[row][col] = "";

        return score;
    }

    private int evaluateBoard(String[][] board) {
        int aiScore = evaluatePlayer(board, AI);
        int playerScore = evaluatePlayer(board, PLAYER);

        // Phòng thủ mạnh hơn một chút để AI không bị người chơi dụ.
        return aiScore - playerScore * 2;
    }

    private int evaluatePlayer(String[][] board, String symbol) {
        int score = 0;

        // Đánh giá theo từng ô.
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (symbol.equals(board[row][col])) {
                    score += evaluatePoint(board, row, col, symbol);
                }
            }
        }

        // Đánh giá thêm theo cửa sổ 5 ô.
        score += evaluateAllWindows(board, symbol);

        return score;
    }

    private int evaluatePoint(String[][] board, int row, int col, String symbol) {
        int score = 0;

        score += evaluateDirection(board, row, col, 0, 1, symbol);
        score += evaluateDirection(board, row, col, 1, 0, symbol);
        score += evaluateDirection(board, row, col, 1, 1, symbol);
        score += evaluateDirection(board, row, col, 1, -1, symbol);

        return score;
    }

    private int evaluateDirection(String[][] board, int row, int col, int dRow, int dCol, String symbol) {
        int count = 1;
        int openEnds = 0;

        int r = row + dRow;
        int c = col + dCol;

        while (isInsideBoard(r, c) && symbol.equals(board[r][c])) {
            count++;
            r += dRow;
            c += dCol;
        }

        if (isInsideBoard(r, c) && isEmpty(board[r][c])) {
            openEnds++;
        }

        r = row - dRow;
        c = col - dCol;

        while (isInsideBoard(r, c) && symbol.equals(board[r][c])) {
            count++;
            r -= dRow;
            c -= dCol;
        }

        if (isInsideBoard(r, c) && isEmpty(board[r][c])) {
            openEnds++;
        }

        return getScoreByCount(count, openEnds);
    }

    private int evaluateAllWindows(String[][] board, String symbol) {
        int score = 0;

        score += evaluateWindowsByDirection(board, symbol, 0, 1);
        score += evaluateWindowsByDirection(board, symbol, 1, 0);
        score += evaluateWindowsByDirection(board, symbol, 1, 1);
        score += evaluateWindowsByDirection(board, symbol, 1, -1);

        return score;
    }

    private int evaluateWindowsByDirection(String[][] board, String symbol, int dRow, int dCol) {
        int score = 0;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int endRow = row + dRow * 4;
                int endCol = col + dCol * 4;

                if (!isInsideBoard(endRow, endCol)) {
                    continue;
                }

                score += scoreWindow(board, row, col, dRow, dCol, symbol);
            }
        }

        return score;
    }

    private int scoreWindow(String[][] board, int row, int col, int dRow, int dCol, String symbol) {
        String opponent = symbol.equals(AI) ? PLAYER : AI;

        int symbolCount = 0;
        int opponentCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < WIN_COUNT; i++) {
            int r = row + dRow * i;
            int c = col + dCol * i;

            if (symbol.equals(board[r][c])) {
                symbolCount++;
            } else if (opponent.equals(board[r][c])) {
                opponentCount++;
            } else {
                emptyCount++;
            }
        }

        if (symbolCount > 0 && opponentCount > 0) {
            return 0;
        }

        if (symbolCount == 5) {
            return 10000000;
        }

        if (symbolCount == 4 && emptyCount == 1) {
            return 700000;
        }

        if (symbolCount == 3 && emptyCount == 2) {
            return 50000;
        }

        if (symbolCount == 2 && emptyCount == 3) {
            return 2000;
        }

        if (symbolCount == 1 && emptyCount == 4) {
            return 50;
        }

        return 0;
    }

    private int getScoreByCount(int count, int openEnds) {
        if (count == 5) {
            return 10000000;
        }

        // Luật của bạn là đúng 5 quân, quá 5 không tính thắng.
        if (count > 5) {
            return 0;
        }

        if (openEnds == 0) {
            return 0;
        }

        if (count == 4) {
            if (openEnds == 2) return 1000000;
            if (openEnds == 1) return 300000;
        }

        if (count == 3) {
            if (openEnds == 2) return 100000;
            if (openEnds == 1) return 10000;
        }

        if (count == 2) {
            if (openEnds == 2) return 3000;
            if (openEnds == 1) return 500;
        }

        if (count == 1) {
            if (openEnds == 2) return 30;
            if (openEnds == 1) return 10;
        }

        return 0;
    }

    private List<int[]> getPossibleMoves(String[][] board) {
        List<int[]> moves = new ArrayList<>();
        boolean hasMove = false;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!isEmpty(board[row][col])) {
                    hasMove = true;
                    break;
                }
            }

            if (hasMove) {
                break;
            }
        }

        if (!hasMove) {
            moves.add(new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2});
            return moves;
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (isEmpty(board[row][col]) && hasNeighbor(board, row, col, 2)) {
                    moves.add(new int[]{row, col});
                }
            }
        }

        return moves;
    }

    private boolean hasNeighbor(String[][] board, int row, int col, int distance) {
        for (int r = row - distance; r <= row + distance; r++) {
            for (int c = col - distance; c <= col + distance; c++) {
                if (isInsideBoard(r, c) && !isEmpty(board[r][c])) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkWin(String[][] board, int row, int col, String symbol) {
        return isExactFive(board, row, col, 0, 1, symbol)
                || isExactFive(board, row, col, 1, 0, symbol)
                || isExactFive(board, row, col, 1, 1, symbol)
                || isExactFive(board, row, col, 1, -1, symbol);
    }

    private boolean isExactFive(String[][] board, int row, int col, int dRow, int dCol, String symbol) {
        int count = 1;

        int r = row + dRow;
        int c = col + dCol;

        while (isInsideBoard(r, c) && symbol.equals(board[r][c])) {
            count++;
            r += dRow;
            c += dCol;
        }

        boolean blockedAfterBySame = isInsideBoard(r, c) && symbol.equals(board[r][c]);

        r = row - dRow;
        c = col - dCol;

        while (isInsideBoard(r, c) && symbol.equals(board[r][c])) {
            count++;
            r -= dRow;
            c -= dCol;
        }

        boolean blockedBeforeBySame = isInsideBoard(r, c) && symbol.equals(board[r][c]);

        return count == WIN_COUNT && !blockedAfterBySame && !blockedBeforeBySame;
    }

    private boolean isBoardFull(String[][] board) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (isEmpty(board[row][col])) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    private boolean isEmpty(String value) {
        return value == null || value.equals("");
    }

    private void normalizeBoard(String[][] board) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col] == null) {
                    board[row][col] = "";
                }
            }
        }
    }
}