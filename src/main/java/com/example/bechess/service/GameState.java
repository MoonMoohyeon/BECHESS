package com.example.bechess.service;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.Position;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@Getter
public class GameState {

    private static final Logger log = LoggerFactory.getLogger(GameState.class);
    private Map<Position, ChessPiece> board = new HashMap<>();
    private String currentPlayer = "WHITE";
    private String currentTeam;
    private String currentRole; // COMMANDER or ACTOR
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteRook1Moved = false;
    private boolean whiteRook2Moved = false;
    private boolean blackRook1Moved = false;
    private boolean blackRook2Moved = false;
    private Position enPassantTarget;

    public GameState() {
        this.board = new HashMap<>();
        this.currentTeam = "A";
        this.currentRole = "COMMANDER";
         initializeBoard();
    }

    public boolean processMoveWEB(Move move) {
        printBoard();
        log.info(String.valueOf(isValidMove(move)));
        if (isValidMove(move)) {
            updateBoard(move);
            switchPlayer();
            switchTurn();
            log.info("현재 정보 : ", getCurrentPlayer(), getCurrentTeam(), getCurrentRole());
            printBoard();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean processMoveVR(Move move) {
        printBoard();
        log.info(String.valueOf(isValidMove(move)));
        if (isValidMove(move)) {
            updateBoard(move);
            switchPlayer();
            switchTurn();
            log.info("현재 정보 : ", getCurrentPlayer(), getCurrentTeam(), getCurrentRole());
            printBoard();
            return true;
        }
        else {
            return false;
        }
    }

    public void printBoard() {
        char[][] visualBoard = new char[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                visualBoard[i][j] = '.';
            }
        }

        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            Position pos = entry.getKey();
            ChessPiece piece = entry.getValue();
            // x축 반전: visualBoard[pos.getY()][7 - pos.getX()]
            visualBoard[pos.getY()][7 - pos.getX()] = getPieceSymbol(piece);
        }

        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                System.out.print(visualBoard[i][j] + " ");
            }
            System.out.println();
        }
    }


    public String getBoardState() {
        StringBuilder boardStringBuilder = new StringBuilder();

        char[][] visualBoard = new char[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                visualBoard[i][j] = '.';
            }
        }

        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            Position pos = entry.getKey();
            ChessPiece piece = entry.getValue();
            // x축 반전: visualBoard[pos.getY()][7 - pos.getX()]
            visualBoard[pos.getY()][7 - pos.getX()] = getPieceSymbol(piece);
        }

        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                boardStringBuilder.append(visualBoard[i][j]).append(" ");
            }
            boardStringBuilder.append("\n");
        }

        return boardStringBuilder.toString();
    }


    private char getPieceSymbol(ChessPiece piece) {
        switch (piece.getType()) {
            case "PAWN": return 'P';
            case "KNIGHT": return 'N';
            case "BISHOP": return 'B';
            case "ROOK": return 'R';
            case "QUEEN": return 'Q';
            case "KING": return 'K';
            default: return '.';
        }
    }


    public void switchPlayer() {
        currentPlayer = currentPlayer.equals("WHITE") ? "BLACK" : "WHITE";
    }

    public void switchTurn() {
        if (currentRole.equals("COMMANDER")) {
            currentRole = "ACTOR";
        } else {
            currentRole = "COMMANDER";
            currentTeam = currentTeam.equals("A") ? "B" : "A";
        }
    }

    public boolean isValidMove(Move move) {
        Position from = move.getFrom();
        log.info("pos = {}, {}", from.getX(), from.getY());
        ChessPiece piece = board.get(from);
        log.info("piece : {}, {}, player : {}", piece.getType(), piece.getColor(), currentPlayer);
        if (piece == null || !piece.getColor().equals(currentPlayer)) {
            return false;
        }
        return piece.isValidMove(move, board, this);
    }

    public void updateBoard(Move move) {
        // 먼저 이동할 기물을 가져옵니다.
        ChessPiece piece = board.get(move.getFrom());
        if (piece == null) {
            log.error("이동하려는 위치에 기물이 없습니다.");
            return;
        }

        // 기물의 위치를 업데이트합니다.
        piece.setPosition(move.getTo());

        // 원래 위치에서 기물을 제거합니다.
        board.remove(move.getFrom());

        // 새로운 위치에 기물을 배치합니다.
        board.put(move.getTo(), piece);

        log.info("piece = {}, {}, {}, {}", piece.getPosition().getX(), piece.getPosition().getY(), piece.getType(), piece.getColor());

        // 기물 잡기 처리
        if (board.containsKey(move.getTo()) && board.get(move.getTo()) != piece) {
            board.remove(move.getTo());
        }

        // 특수 이동 처리 (캐슬링, 앙파상, 프로모션 등)
        handleSpecialMoves(move, piece);
    }

    private void handleSpecialMoves(Move move, ChessPiece piece) {
        // KING 이동에 따른 캐슬링 불가 처리 및 캐슬링 이동 처리
        if (piece.getType().equals("KING")) {
            if (currentPlayer.equals("WHITE")) {
                whiteKingMoved = true;
                // 캐슬링: 킹이 두 칸 움직이면 룩도 같이 움직임
                if (move.getFrom().equals(new Position(3, 0)) && move.getTo().equals(new Position(1, 0))) { // 킹사이드 캐슬링
                    ChessPiece rook = board.remove(new Position(0, 0));
                    rook.setPosition(new Position(2, 0));
                    board.put(new Position(2, 0), rook);
                } else if (move.getFrom().equals(new Position(3, 0)) && move.getTo().equals(new Position(5, 0))) { // 퀸사이드 캐슬링
                    ChessPiece rook = board.remove(new Position(7, 0));
                    rook.setPosition(new Position(4, 0));
                    board.put(new Position(4, 0), rook);
                }
            } else {
                blackKingMoved = true;
                // 캐슬링: 킹이 두 칸 움직이면 룩도 같이 움직임
                if (move.getFrom().equals(new Position(3, 7)) && move.getTo().equals(new Position(1, 7))) { // 킹사이드 캐슬링
                    ChessPiece rook = board.remove(new Position(0, 7));
                    rook.setPosition(new Position(2, 7));
                    board.put(new Position(2, 7), rook);
                } else if (move.getFrom().equals(new Position(3, 7)) && move.getTo().equals(new Position(5, 7))) { // 퀸사이드 캐슬링
                    ChessPiece rook = board.remove(new Position(7, 7));
                    rook.setPosition(new Position(4, 7));
                    board.put(new Position(4, 7), rook);
                }
            }
        }

        // ROOK 이동에 따른 캐슬링 불가 처리
        else if (piece.getType().equals("ROOK")) {
            if (currentPlayer.equals("WHITE")) {
                if (move.getFrom().equals(new Position(0, 0))) whiteRook1Moved = true;
                if (move.getFrom().equals(new Position(7, 0))) whiteRook2Moved = true;
            } else {
                if (move.getFrom().equals(new Position(0, 7))) blackRook1Moved = true;
                if (move.getFrom().equals(new Position(7, 7))) blackRook2Moved = true;
            }
        }

        // PAWN 이동에 따른 앙파상 처리 및 프로모션 처리
        else if (piece.getType().equals("PAWN")) {
            // 앙파상 타겟 설정
            if (Math.abs(move.getFrom().getY() - move.getTo().getY()) == 2) {
                enPassantTarget = new Position(move.getTo().getX(), (move.getFrom().getY() + move.getTo().getY()) / 2);
            } else {
                enPassantTarget = null;
            }

            // 앙파상으로 상대 폰 잡기
            if (move.getTo().equals(enPassantTarget)) {
                Position capturePosition = new Position(move.getTo().getX(), move.getFrom().getY());
                board.remove(capturePosition);
            }

            // 프로모션 처리 (디폴트로 퀸으로 프로모션)
            if ((move.getTo().getY() == 7 && piece.getColor().equals("WHITE")) || (move.getTo().getY() == 0 && piece.getColor().equals("BLACK"))) {
                piece.setType("QUEEN");
            }
        }
    }


    public void initializeBoard() {
        // Initialize white pieces
        board.put(new Position(0, 0), new ChessPiece("ROOK", "WHITE", new Position(0, 0)));
        board.put(new Position(1, 0), new ChessPiece("KNIGHT", "WHITE", new Position(1, 0)));
        board.put(new Position(2, 0), new ChessPiece("BISHOP", "WHITE", new Position(2, 0)));
        board.put(new Position(3, 0), new ChessPiece("KING", "WHITE", new Position(3, 0)));
        board.put(new Position(4, 0), new ChessPiece("QUEEN", "WHITE", new Position(4, 0)));
        board.put(new Position(5, 0), new ChessPiece("BISHOP", "WHITE", new Position(5, 0)));
        board.put(new Position(6, 0), new ChessPiece("KNIGHT", "WHITE", new Position(6, 0)));
        board.put(new Position(7, 0), new ChessPiece("ROOK", "WHITE", new Position(7, 0)));
        for (int i = 0; i < 8; i++) {
            board.put(new Position(i, 1), new ChessPiece("PAWN", "WHITE", new Position(i, 1)));
        }

        // Initialize black pieces
        board.put(new Position(0, 7), new ChessPiece("ROOK", "BLACK", new Position(0, 7)));
        board.put(new Position(1, 7), new ChessPiece("KNIGHT", "BLACK", new Position(1, 7)));
        board.put(new Position(2, 7), new ChessPiece("BISHOP", "BLACK", new Position(2, 7)));
        board.put(new Position(3, 7), new ChessPiece("KING", "BLACK", new Position(3, 7)));
        board.put(new Position(4, 7), new ChessPiece("QUEEN", "BLACK", new Position(4, 7)));
        board.put(new Position(5, 7), new ChessPiece("BISHOP", "BLACK", new Position(5, 7)));
        board.put(new Position(6, 7), new ChessPiece("KNIGHT", "BLACK", new Position(6, 7)));
        board.put(new Position(7, 7), new ChessPiece("ROOK", "BLACK", new Position(7, 7)));
        for (int i = 0; i < 8; i++) {
            board.put(new Position(i, 6), new ChessPiece("PAWN", "BLACK", new Position(i, 6)));
        }
    }

}
