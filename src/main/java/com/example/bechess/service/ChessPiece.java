package com.example.bechess.service;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.Position;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public class ChessPiece {
    @Setter
    private String type;
    private String color;
    @Setter
    private Position position;

    public ChessPiece(String type, String color, Position position) {
        this.type = type;
        this.color = color;
        this.position = position;
    }

    public boolean isValidMove(Move move, Map<Position, ChessPiece> board, GameState gameState) {
        switch (type) {
            case "PAWN":
                return isValidPawnMove(move, board, gameState);
            case "ROOK":
                return isValidRookMove(move, board);
            case "KNIGHT":
                return isValidKnightMove(move, board);
            case "BISHOP":
                return isValidBishopMove(move, board);
            case "QUEEN":
                return isValidQueenMove(move, board);
            case "KING":
                return isValidKingMove(move, board, gameState);
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(Move move, Map<Position, ChessPiece> board, GameState gameState) {
        int direction = color.equals("WHITE") ? 1 : -1;
        Position from = move.getFrom();
        Position to = move.getTo();
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        if (dx == 0 && dy == direction && !board.containsKey(to)) {
            return true; // Single move forward
        }
        if (dx == 0 && dy == 2 * direction && !board.containsKey(to) && !board.containsKey(new Position(from.getX(), from.getY() + direction))) {
            return from.getY() == (color.equals("WHITE") ? 1 : 6); // Initial double move forward
        }
        if (Math.abs(dx) == 1 && dy == direction && board.containsKey(to) && !board.get(to).getColor().equals(color)) {
            return true; // Capturing move
        }
        if (Math.abs(dx) == 1 && dy == direction && to.equals(gameState.getEnPassantTarget())) {
            return true; // En passant capture
        }
        return false;
    }

    private boolean isValidRookMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        if (from.getX() != to.getX() && from.getY() != to.getY()) {
            return false;
        }
        return isPathClear(from, to, board);
    }

    private boolean isValidKnightMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        int dx = Math.abs(to.getX() - from.getX());
        int dy = Math.abs(to.getY() - from.getY());
        return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
    }

    private boolean isValidBishopMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        if (Math.abs(to.getX() - from.getX()) != Math.abs(to.getY() - from.getY())) {
            return false;
        }
        return isPathClear(from, to, board);
    }

    private boolean isValidQueenMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        if (from.getX() == to.getX() || from.getY() == to.getY()) {
            return isPathClear(from, to, board);
        }
        if (Math.abs(to.getX() - from.getX()) == Math.abs(to.getY() - from.getY())) {
            return isPathClear(from, to, board);
        }
        return false;
    }

    private boolean isValidKingMove(Move move, Map<Position, ChessPiece> board, GameState gameState) {
        Position from = move.getFrom();
        Position to = move.getTo();
        int dx = Math.abs(to.getX() - from.getX());
        int dy = Math.abs(to.getY() - from.getY());
        if (dx <= 1 && dy <= 1) {
            return true; // Normal move
        }

        // Castling
        if (dx == 2 && dy == 0) {
            if (color.equals("WHITE") && !gameState.isWhiteKingMoved() &&
                    ((to.getX() == 6 && !gameState.isWhiteRook2Moved() && isPathClear(from, new Position(7, 0), board)) ||
                            (to.getX() == 2 && !gameState.isWhiteRook1Moved() && isPathClear(from, new Position(0, 0), board)))) {
                return true;
            }
            if (color.equals("BLACK") && !gameState.isBlackKingMoved() &&
                    ((to.getX() == 6 && !gameState.isBlackRook2Moved() && isPathClear(from, new Position(7, 7), board)) ||
                            (to.getX() == 2 && !gameState.isBlackRook1Moved() && isPathClear(from, new Position(0, 7), board)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPathClear(Position from, Position to, Map<Position, ChessPiece> board) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int x = from.getX() + dx;
        int y = from.getY() + dy;

        while (x != to.getX() || y != to.getY()) {
            if (board.containsKey(new Position(x, y))) {
                return false;
            }
            x += dx;
            y += dy;
        }
        return true;
    }
}
