package com.example.bechess.service;

import com.example.bechess.dto.ChessPiece;
import com.example.bechess.dto.Position;
import lombok.Getter;

@Getter
class PreviousMove {
    private Position from;
    private Position to;
    private ChessPiece movedPiece;
    private ChessPiece capturedPiece;
    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteRook1Moved;
    private boolean whiteRook2Moved;
    private boolean blackRook1Moved;
    private boolean blackRook2Moved;
    private Position enPassantTarget;

    public PreviousMove(Position from, Position to, ChessPiece movedPiece, ChessPiece capturedPiece, boolean whiteKingMoved, boolean blackKingMoved, boolean whiteRook1Moved, boolean whiteRook2Moved, boolean blackRook1Moved, boolean blackRook2Moved, Position enPassantTarget) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.whiteKingMoved = whiteKingMoved;
        this.blackKingMoved = blackKingMoved;
        this.whiteRook1Moved = whiteRook1Moved;
        this.whiteRook2Moved = whiteRook2Moved;
        this.blackRook1Moved = blackRook1Moved;
        this.blackRook2Moved = blackRook2Moved;
        this.enPassantTarget = enPassantTarget;
    }
}
