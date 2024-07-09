package com.example.bechess.service;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.Position;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class GameState {

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
        // initializeBoard();
    }

    public boolean isValidMove(Move move) {
        ChessPiece piece = board.get(move.getFrom());
        if (piece == null || !piece.getColor().equals(currentPlayer)) {
            return false;
        }
        return piece.isValidMove(move, board, this);
    }

    public void updateBoard(Move move) {
        ChessPiece piece = board.remove(move.getFrom());
        piece.setPosition(move.getTo());
        board.put(move.getTo(), piece);

        // Handle capturing
        if (board.containsKey(move.getTo())) {
            board.remove(move.getTo());
        }

        // Handle special moves
        if (piece.getType().equals("KING")) {
            if (currentPlayer.equals("WHITE")) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        } else if (piece.getType().equals("ROOK")) {
            if (currentPlayer.equals("WHITE")) {
                if (move.getFrom().equals(new Position(0, 0))) whiteRook1Moved = true;
                if (move.getFrom().equals(new Position(7, 0))) whiteRook2Moved = true;
            } else {
                if (move.getFrom().equals(new Position(0, 7))) blackRook1Moved = true;
                if (move.getFrom().equals(new Position(7, 7))) blackRook2Moved = true;
            }
        } else if (piece.getType().equals("PAWN")) {
            if (Math.abs(move.getFrom().getY() - move.getTo().getY()) == 2) {
                enPassantTarget = new Position(move.getTo().getX(), (move.getFrom().getY() + move.getTo().getY()) / 2);
            } else {
                enPassantTarget = null;
            }
            if (move.getTo().equals(enPassantTarget)) {
                Position capturePosition = new Position(move.getTo().getX(), move.getFrom().getY());
                board.remove(capturePosition);
            }
            if ((move.getTo().getY() == 7 && piece.getColor().equals("WHITE")) || (move.getTo().getY() == 0 && piece.getColor().equals("BLACK"))) {
                piece.setType("QUEEN");
            }
        }
    }

    public void switchPlayer() {
        currentPlayer = currentPlayer.equals("WHITE") ? "BLACK" : "WHITE";
    }

//    private void initializeBoard() {
//        // Initialize white pieces
//        board.put(new Position(0, 0), new ChessPiece("ROOK", "WHITE", new Position(0, 0)));
//        board.put(new Position(1, 0), new ChessPiece("KNIGHT", "WHITE", new Position(1, 0)));
//        board.put(new Position(2, 0), new ChessPiece("BISHOP", "WHITE", new Position(2, 0)));
//        board.put(new Position(3, 0), new ChessPiece("QUEEN", "WHITE", new Position(3, 0)));
//        board.put(new Position(4, 0), new ChessPiece("KING", "WHITE", new Position(4, 0)));
//        board.put(new Position(5, 0), new ChessPiece("BISHOP", "WHITE", new Position(5, 0)));
//        board.put(new Position(6, 0), new ChessPiece("KNIGHT", "WHITE", new Position(6, 0)));
//        board.put(new Position(7, 0), new ChessPiece("ROOK", "WHITE", new Position(7, 0)));
//        for (int i = 0; i < 8; i++) {
//            board.put(new Position(i, 1), new ChessPiece("PAWN", "WHITE", new Position(i, 1)));
//        }
//
//        // Initialize black pieces
//        board.put(new Position(0, 7), new ChessPiece("ROOK", "BLACK", new Position(0, 7)));
//        board.put(new Position(1, 7), new ChessPiece("KNIGHT", "BLACK", new Position(1, 7)));
//        board.put(new Position(2, 7), new ChessPiece("BISHOP", "BLACK", new Position(2, 7)));
//        board.put(new Position(3, 7), new ChessPiece("QUEEN", "BLACK", new Position(3, 7)));
//        board.put(new Position(4, 7), new ChessPiece("KING", "BLACK", new Position(4, 7)));
//        board.put(new Position(5, 7), new ChessPiece("BISHOP", "BLACK", new Position(5, 7)));
//        board.put(new Position(6, 7), new ChessPiece("KNIGHT", "BLACK", new Position(6, 7)));
//        board.put(new Position(7, 7), new ChessPiece("ROOK", "BLACK", new Position(7, 7)));
//        for (int i = 0; i < 8; i++) {
//            board.put(new Position(i, 6), new ChessPiece("PAWN", "BLACK", new Position(i, 6)));
//        }
//    }

    public void switchTurn() {
        if (currentRole.equals("COMMANDER")) {
            currentRole = "ACTOR";
        } else {
            currentRole = "COMMANDER";
            currentTeam = currentTeam.equals("A") ? "B" : "A";
        }
    }

}
