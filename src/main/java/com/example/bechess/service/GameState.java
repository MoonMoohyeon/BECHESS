package com.example.bechess.service;

import com.example.bechess.dto.ChessPiece;
import com.example.bechess.dto.Move;
import com.example.bechess.dto.Position;
import lombok.Getter;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Getter
@Setter
public class GameState {

    private static final Logger log = LoggerFactory.getLogger(GameState.class);
    private Map<Position, ChessPiece> board;
    private String currentPlayer;
    private String currentRole;

    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteRook1Moved;
    private boolean whiteRook2Moved;
    private boolean blackRook1Moved;
    private boolean blackRook2Moved;
    private Position enPassantTarget;
    private String enPassantTargetColor;

    private Stack<PreviousMove> moveHistory;

    private Move Webmove;
    private Move VRmove;

    private boolean checkmated;
    private boolean enpassantMoved;
    private Position castledRook;
    private Position promotion;

    public GameState() {
         initializeBoard();
    }

    public boolean processMoveWEB(Move move) {

        if (currentRole.equals("COMMANDER")) {

            switchPlayer();
//        if (isCheckmate()) {
//            log.info("체크메이트! 게임 종료.");
//            return false;  // 체크메이트 상태이므로 이동 불가
//        }

            Webmove = move;

            ChessPiece movedPiece = board.get(move.getFrom());
//            ChessPiece capturedPiece = board.get(move.getTo());

            Position to = move.getTo();
//            Position from = move.getFrom();

            if (movedPiece == null || !movedPiece.getColor().equals(currentPlayer)) {
                return false; // 기물이 없거나 상대 기물인 경우
            }

            // 이동하려는 위치에 아군 기물이 있으면 이동 불가
            if (board.containsKey(to) && board.get(to).getColor().equals(currentPlayer)) {
                return false;
            }

            if (isValidMove(move, board)) {

                updateBoard(move, board);

                if (isKingUnderAttack(currentPlayer, board)) {
                    undoLastMove();
                    return false; // 킹이 체크 상태에 빠지는 이동은 유효하지 않음
                }

                getBoardState(board);

                return true;
            }

            return false;
        }
        return false;
    }

    public boolean processMoveVR(Move move, String battle) {

        if (currentRole.equals("ACTOR")) {
            VRmove = move;

            if (Webmove != VRmove) {
                undoLastMove();
            } else {
                return true;
            }
            if(battle.equals("LOSE")) {
                // 원래 위치에서 기물 제거
                board.remove(move.getFrom());
            }
            else if (isValidMove(move, board)) {
                updateBoard(move, board);
                switchPlayer();
                if(move.getColor().equals("BLACK")) {
                    switchTurn();
                }

                log.info("현재 정보 : " + getCurrentPlayer() + getCurrentRole());
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

//    public void processPromotion(Position promotionPosition, char promotion) {
//        // 이동할 기물을 가져옴
//        ChessPiece piece = board.get(promotionPosition);
//        if (piece == null) {
//            log.error("이동하려는 위치에 기물이 없습니다.");
//            return;
//        }
//
//        if (promotion == 'q') {
//            piece.setType("QUEEN");
//        }
//        else if (promotion == 'r') {
//            piece.setType("ROOK");
//        }
//        else if (promotion == 'b') {
//            piece.setType("BISHOP");
//        }
//        else if (promotion == 'n') {
//            piece.setType("KNIGHT");
//        }
//    }

    public void updateBoard(Move move, Map<Position, ChessPiece> board) {

        ChessPiece movedPiece = board.get(move.getFrom());
        ChessPiece capturedPiece = board.get(move.getTo());

        moveHistory.push(new PreviousMove(
                move.getFrom(),
                move.getTo(),
                movedPiece,
                capturedPiece,
                whiteKingMoved,
                blackKingMoved,
                whiteRook1Moved,
                whiteRook2Moved,
                blackRook1Moved,
                blackRook2Moved,
                enPassantTarget
        ));

        // 이동할 기물을 가져옴
        ChessPiece piece = board.get(move.getFrom());
        if (piece == null) {
            log.error("이동하려는 위치에 기물이 없습니다.");
            return;
        }

        // 특수 이동 처리 (캐슬링, 앙파상, 프로모션 등)
        handleSpecialMoves(move, piece);

        // 프로모션 처리
        if (piece.getType().equals("PAWN") &&
                (move.getTo().getY() == 7 || move.getTo().getY() == 0)) {
            log.info("promotion : " + piece.getType());
            piece.setType("QUEEN");  // 기본적으로 퀸으로 프로모션
            promotion = move.getTo();
        }

        // 기물의 위치 업데이트
        piece.setPosition(move.getTo());

        // 원래 위치에서 기물 제거
        board.remove(move.getFrom());

        // 기물 잡기 처리
        if (board.containsKey(move.getTo()) && board.get(move.getTo()) != piece) {
            board.remove(move.getTo());
        }

        // 앙파상으로 상대 폰을 잡은 경우 처리
//        if (piece.getType().equals("PAWN") && move.getTo().equals(enPassantTarget)) {
//            Position capturePosition = new Position(move.getTo().getX(), move.getFrom().getY());
//            board.remove(capturePosition);  // 앙파상으로 잡힌 폰을 제거
//        }

        // 새로운 위치에 기물 배치
        board.put(move.getTo(), piece);
    }

    public void simulateBoard(Move move, Map<Position, ChessPiece> board) {

        ChessPiece movedPiece = board.get(move.getFrom());
        ChessPiece capturedPiece = board.get(move.getTo());

        // 이동할 기물을 가져옴
        ChessPiece piece = board.get(move.getFrom());
        if (piece == null) {
            log.error("이동하려는 위치에 기물이 없습니다.");
            return;
        }

        // 특수 이동 처리 (캐슬링, 앙파상, 프로모션 등)
        handleSpecialMoves(move, piece);

        // 프로모션 처리
        if (piece.getType().equals("PAWN") &&
                (move.getTo().getY() == 7 || move.getTo().getY() == 0)) {
            log.info("promotion : " + piece.getType());
            piece.setType("QUEEN");  // 기본적으로 퀸으로 프로모션
            promotion = move.getTo();
        }

        // 기물의 위치 업데이트
        piece.setPosition(move.getTo());

        // 원래 위치에서 기물 제거
        board.remove(move.getFrom());

        // 기물 잡기 처리
        if (board.containsKey(move.getTo()) && board.get(move.getTo()) != piece) {
            board.remove(move.getTo());
        }

        // 앙파상으로 상대 폰을 잡은 경우 처리
//        if (piece.getType().equals("PAWN") && move.getTo().equals(enPassantTarget)) {
//            Position capturePosition = new Position(move.getTo().getX(), move.getFrom().getY());
//            board.remove(capturePosition);  // 앙파상으로 잡힌 폰을 제거
//        }

        // 새로운 위치에 기물 배치
        board.put(move.getTo(), piece);
    }

    public String getBoardState(Map<Position, ChessPiece> board) {
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
            visualBoard[pos.getY()][7 - pos.getX()] = getPieceSymbol(piece);
        }

        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                System.out.print(visualBoard[i][j] + " ");
            }
            System.out.println();
        }

        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                boardStringBuilder.append(visualBoard[i][j]).append(" ");
            }
            boardStringBuilder.append("\n");
        }

        return boardStringBuilder.toString();
    }

    public boolean isValidMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        ChessPiece piece = board.get(from);

//        log.info("from : " + from.getX() + from.getY() + " to : " + to.getX() + to.getY() + " piece : " + piece.getType() + " color : " + piece.getColor());

        // 각 기물의 타입에 따른 유효한 이동 체크
        switch (piece.getType()) {
            case "PAWN" -> {
                return isValidPawnMove(move, board);
            }
            case "ROOK" -> {
                return isValidRookMove(move);
            }
            case "KNIGHT" -> {
                return isValidKnightMove(move);
            }
            case "BISHOP" -> {
                return isValidBishopMove(move);
            }
            case "QUEEN" -> {
                return isValidQueenMove(move);
            }
            case "KING" -> {
                return isValidKingMove(move, board);
            }
            default -> {
                return false;
            }
        }
    }

        // 폰의 유효한 이동 체크
        public boolean isValidPawnMove(Move move, Map<Position, ChessPiece> board) {
            Position from = move.getFrom();
            Position to = move.getTo();
            ChessPiece piece = board.get(from);

            int dx = Math.abs(to.getX() - from.getX());
            int dy = to.getY() - from.getY();
            int direction = piece.getColor().equals("WHITE") ? 1 : -1;

            // 일반 이동
            if (dx == 0 && dy == direction && !board.containsKey(to)) {
                return true;
            }

            // 첫 이동 시 두 칸 전진
            if (dx == 0 && dy == 2 * direction && from.getY() == (piece.getColor().equals("WHITE") ? 1 : 6) && !board.containsKey(to)) {
                return isPathClear(from, to, board);
            }

            // 대각선 공격
            if (dx == 1 && dy == direction && board.containsKey(to) && !board.get(to).getColor().equals(piece.getColor())) {
                return true;
            }

            // 앙파상으로 상대 폰 잡기
            if(enPassantTarget != null) {
                log.info("enpassant : " + enPassantTarget.getX() + ", " + enPassantTarget.getY() + enPassantTargetColor);
                if (move.getTo().getX() == enPassantTarget.getX() && move.getTo().getY() == enPassantTarget.getY() && move.getColor() != enPassantTargetColor) {
                    enpassantMoved = true;
                    Position capturePosition = new Position(move.getTo().getX(), move.getFrom().getY());
                    board.remove(capturePosition);
                    return true;
                }
            }

            return false;
        }

        // 룩의 유효한 이동 체크
        public boolean isValidRookMove(Move move) {
            Position from = move.getFrom();
            Position to = move.getTo();

            if (from.getX() == to.getX() || from.getY() == to.getY()) {
                return isPathClear(from, to, board);
            }
            return false;
        }

        // 나이트의 유효한 이동 체크
        public boolean isValidKnightMove(Move move) {
            Position from = move.getFrom();
            Position to = move.getTo();
            int dx = Math.abs(to.getX() - from.getX());
            int dy = Math.abs(to.getY() - from.getY());

            return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
        }

        // 비숍의 유효한 이동 체크
        public boolean isValidBishopMove(Move move) {
            Position from = move.getFrom();
            Position to = move.getTo();
            int dx = Math.abs(to.getX() - from.getX());
            int dy = Math.abs(to.getY() - from.getY());

            return dx == dy && isPathClear(from, to, board);
        }

        // 퀸의 유효한 이동 체크
        public boolean isValidQueenMove(Move move) {
            Position from = move.getFrom();
            Position to = move.getTo();

            int dx = Math.abs(to.getX() - from.getX());
            int dy = Math.abs(to.getY() - from.getY());

            return (dx == dy || from.getX() == to.getX() || from.getY() == to.getY()) && isPathClear(from, to, board);
        }

    public boolean isValidKingMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        int dx = Math.abs(to.getX() - from.getX());
        int dy = Math.abs(to.getY() - from.getY());

        // Normal King move (one square in any direction)
        if (dx <= 1 && dy <= 1) {
            // Check if destination square is either empty or occupied by an opponent's piece
            ChessPiece destinationPiece = board.get(to);
            if (destinationPiece == null || !destinationPiece.getColor().equals(getCurrentPlayer())) {
                return true;
            }
        }

        // Castling move
        if (dx == 2 && dy == 0) {
            if (getCurrentPlayer().equals("WHITE") && !whiteKingMoved &&
                    ((to.getX() == 5 && !whiteRook2Moved && isPathClear(from, new Position(7, 0), board) ||
                            (to.getX() == 1 && !whiteRook1Moved && isPathClear(from, new Position(0, 0), board))))) {
                return true;
            }
            if (getCurrentPlayer().equals("BLACK") && !blackKingMoved &&
                    ((to.getX() == 5 && !blackRook2Moved && isPathClear(from, new Position(7, 7), board) ||
                            (to.getX() == 1 && !blackRook1Moved && isPathClear(from, new Position(0, 7), board))))) {
                return true;
            }
        }

        return false;
    }

    private void handleSpecialMoves(Move move, ChessPiece piece) {
        // KING 이동에 따른 캐슬링 불가 처리 및 캐슬링 이동 처리
        switch (piece.getType()) {
            case "KING" -> {
                if (currentPlayer.equals("WHITE")) {
                    whiteKingMoved = true;
                    // 캐슬링: 킹이 두 칸 움직이면 룩도 같이 움직임
                    if (move.getFrom().equals(new Position(3, 0)) && move.getTo().equals(new Position(1, 0))) { // 킹사이드 캐슬링
                        ChessPiece rook = board.remove(new Position(0, 0));
                        rook.setPosition(new Position(2, 0));
                        castledRook = new Position(2,0);
                        board.put(new Position(2, 0), rook);
                    } else if (move.getFrom().equals(new Position(3, 0)) && move.getTo().equals(new Position(5, 0))) { // 퀸사이드 캐슬링
                        ChessPiece rook = board.remove(new Position(7, 0));
                        rook.setPosition(new Position(4, 0));
                        castledRook = new Position(4,0);
                        board.put(new Position(4, 0), rook);
                    }
                } else {
                    blackKingMoved = true;
                    // 캐슬링: 킹이 두 칸 움직이면 룩도 같이 움직임
                    if (move.getFrom().equals(new Position(3, 7)) && move.getTo().equals(new Position(1, 7))) { // 킹사이드 캐슬링
                        ChessPiece rook = board.remove(new Position(0, 7));
                        rook.setPosition(new Position(2, 7));
                        castledRook = new Position(2,7);
                        board.put(new Position(2, 7), rook);
                    } else if (move.getFrom().equals(new Position(3, 7)) && move.getTo().equals(new Position(5, 7))) { // 퀸사이드 캐슬링
                        ChessPiece rook = board.remove(new Position(7, 7));
                        rook.setPosition(new Position(4, 7));
                        castledRook = new Position(4,7);
                        board.put(new Position(4, 7), rook);
                    }
                }
            }

            // ROOK 이동에 따른 캐슬링 불가 처리
            case "ROOK" -> {
                if (currentPlayer.equals("WHITE")) {
                    if (move.getFrom().equals(new Position(0, 0))) whiteRook1Moved = true;
                    if (move.getFrom().equals(new Position(7, 0))) whiteRook2Moved = true;
                } else {
                    if (move.getFrom().equals(new Position(0, 7))) blackRook1Moved = true;
                    if (move.getFrom().equals(new Position(7, 7))) blackRook2Moved = true;
                }
            }

            // PAWN 이동에 따른 앙파상 처리 및 프로모션 처리
            case "PAWN" -> {
                // 앙파상 타겟 설정
                if (Math.abs(move.getFrom().getY() - move.getTo().getY()) == 2) {
                    enPassantTarget = new Position(move.getTo().getX(), (move.getFrom().getY() + move.getTo().getY()) / 2);
                    enPassantTargetColor = move.getColor();
                }
                            // 프로모션 처리 (디폴트로 퀸으로 프로모션)
//                if ((move.getTo().getY() == 7 && piece.getColor().equals("WHITE")) || (move.getTo().getY() == 0 && piece.getColor().equals("BLACK"))) {
//                    piece.setType("QUEEN");
//                    board.put(move.getTo(), piece);
//                }
            }
        }
    }

    // 경로가 비어있는지 확인하는 헬퍼 메서드
    private boolean isPathClear(Position from, Position to, Map<Position, ChessPiece> board) {
        int xDirection = Integer.compare(to.getX(), from.getX());
        int yDirection = Integer.compare(to.getY(), from.getY());

        int x = from.getX() + xDirection;
        int y = from.getY() + yDirection;

        while (x != to.getX() || y != to.getY()) {
            Position intermediate = new Position(x, y);
            if (board.containsKey(intermediate)) {
                return false;
            }
            x += xDirection;
            y += yDirection;
        }
        return true;
    }

    public boolean isCheckmate() {
        // 킹의 위치를 가져옴
        Position kingPosition = findKingPosition(currentPlayer, board);

        // 현재 플레이어의 킹이 공격받는지 확인
        if (!isKingUnderAttack(currentPlayer, board)) {
            return false; // 킹이 공격받지 않는다면 체크메이트가 아님
        }

        // 1. 킹이 이동할 수 있는 모든 위치를 확인
        List<Position> possibleMoves = getPossibleKingMoves(kingPosition);

        // 2. 킹이 이동할 수 있는 각 위치에 대해 이동 후 킹이 공격받는지 확인
        for (Position move : possibleMoves) {

            log.info("possible king moves : " + move.getX() + move.getY());
            Move tempmove = new Move(kingPosition, move);
            Map<Position, ChessPiece> tempboard = board;
            simulateBoard(tempmove, tempboard);

            // 이동 후 공격받는지 확인
            boolean kingIsSafe = !isKingUnderAttack(currentPlayer, tempboard);

            if (kingIsSafe) {
                return false; // 킹이 피할 수 있는 위치가 하나라도 있다면 체크메이트 아님
            }
        }

        // 3. 다른 기물을 이동시켜 체크를 피할 수 있는지 확인
        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();

            // 현재 플레이어의 기물만 고려
            if (piece.getColor().equals(currentPlayer)) {
                List<Position> legalMoves = getLegalMoves(piece.getPosition());

                for (Position to : legalMoves) {
                    log.info("possible legal moves : " + to.getX() + to.getY() + " " + piece.getType());
                    Move tempmove = new Move(piece.getPosition(), to);
                    Map<Position, ChessPiece> tempboard = board;
                    simulateBoard(tempmove, tempboard);

                    boolean kingIsSafe = !isKingUnderAttack(currentPlayer, tempboard);

                    if (kingIsSafe) {
                        log.info("tox : " + to.getX() + "toy: " + to.getY() + " " + piece.getType() + piece.getPosition().getX() + piece.getPosition().getY());
                        return false; // 킹이 체크를 피할 수 있다면 체크메이트 아님
                    }
                }
            }
        }

        log.info("체크메이트");
        checkmated = true;
        return true; // 모든 경우를 시도해도 피할 수 없다면 체크메이트
    }

    private List<Position> getPossibleKingMoves(Position kingPosition) {
        List<Position> possibleMoves = new ArrayList<>();

        int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},          {0, 1},
                {1, -1}, {1, 0}, {1, 1}
        };

        for (int[] direction : directions) {
            int newX = kingPosition.getX() + direction[0];
            int newY = kingPosition.getY() + direction[1];
            Position newPosition = new Position(newX, newY);

            // 체스판 범위 내의 위치만 추가
            if (isPositionOnBoard(newPosition) && isValidKingMove(new Move(kingPosition, newPosition), board)) {
                    possibleMoves.add(newPosition);
            }
        }

        return possibleMoves;
    }

    private boolean isPositionOnBoard(Position position) {
        return position.getX() >= 0 && position.getX() < 8 && position.getY() >= 0 && position.getY() < 8;
    }

    private List<Position> getLegalMoves(Position from) {
        List<Position> legalMoves = new ArrayList<>();
        ChessPiece piece = board.get(from);

        if (piece == null) {
            return legalMoves;
        }

        // 체스판의 모든 위치를 검사하여 유효한 이동인지 확인
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Position to = new Position(x, y);
                Move potentialMove = new Move(from, to, piece.getColor(), piece.getType());

                // 이동이 유효하고 자기 기물이 없는 위치만 추가
                if (isValidMove(potentialMove, board) && (!board.containsKey(to) || !board.get(to).getColor().equals(currentPlayer))) {
                    legalMoves.add(to);
                }
            }
        }

        return legalMoves;
    }

    // 킹의 위치 찾기
    private Position findKingPosition(String color, Map<Position, ChessPiece> board) {
        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();
//            log.info("entry : " + entry.getKey().getX() + entry.getKey().getY());
            if (piece.getType().equals("KING") && piece.getColor().equals(color)) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("킹이 존재하지 않습니다.");  // 킹이 반드시 존재해야 함
    }

    public boolean isKingUnderAttack(String kingcolor, Map<Position, ChessPiece> board) {
        Position kingPosition = findKingPosition(kingcolor, board);
        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();

            // Only consider the opponent's pieces
            if (!piece.getColor().equals(currentPlayer)) {
                Move potentialMove = new Move(entry.getKey(), kingPosition, entry.getValue().getColor(), entry.getValue().getType());
                if (isValidMove(potentialMove, board)) {
                    log.info("king : " + potentialMove.getTo().getX() + potentialMove.getTo().getY() + " type and color : " + potentialMove.getType() + potentialMove.getColor() + " from : "  + potentialMove.getFrom().getX() + potentialMove.getFrom().getY());
                    log.info("King is under attack!");
                    return true; // The king is under attack by an opponent's piece
                }
            }
        }
        return false; // The king is not under attack
    }


    public void undoLastMove() {
        if (moveHistory.isEmpty()) {
            log.warn("되돌릴 수 있는 이동이 없습니다.");
            return;
        }

        // 마지막 이동을 꺼냄
        PreviousMove lastMove = moveHistory.pop();

        // 기물의 위치를 되돌림
        ChessPiece movedPiece = lastMove.getMovedPiece();
        ChessPiece capturedPiece = lastMove.getCapturedPiece();

        board.remove(lastMove.getTo());  // 마지막 이동에서 기물이 있었던 위치 제거
        board.put(lastMove.getFrom(), movedPiece);  // 기물을 원래 위치로 복구
        movedPiece.setPosition(lastMove.getFrom());

        // 잡힌 기물이 있으면 복구
        if (capturedPiece != null) {
            board.put(lastMove.getTo(), capturedPiece);
        }

        // 특수한 상태 복구
        whiteKingMoved = lastMove.isWhiteKingMoved();
        blackKingMoved = lastMove.isBlackKingMoved();
        whiteRook1Moved = lastMove.isWhiteRook1Moved();
        whiteRook2Moved = lastMove.isWhiteRook2Moved();
        blackRook1Moved = lastMove.isBlackRook1Moved();
        blackRook2Moved = lastMove.isBlackRook2Moved();
        enPassantTarget = lastMove.getEnPassantTarget();

        // 턴을 원래 플레이어로 되돌림
//        switchPlayer();

        log.info("이전 턴을 되돌렸습니다.");
    }


    private char getPieceSymbol(ChessPiece piece) {
        switch (piece.getType()) {
            case "PAWN" -> {
                return 'P';
            }
            case "KNIGHT" -> {
                return 'N';
            }
            case "BISHOP" -> {
                return 'B';
            }
            case "ROOK" -> {
                return 'R';
            }
            case "QUEEN" -> {
                return 'Q';
            }
            case "KING" -> {
                return 'K';
            }
            default -> {
                return '.';
            }
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
        }
    }

    private Position VRcoordinate(int x, int y) {
        int rx = 0, ry = 0;

        // y 좌표에 대한 switch
        switch ((y + 3500) / 1000) {
            case 0 -> ry = 0;
            case 1 -> ry = 1;
            case 2 -> ry = 2;
            case 3 -> ry = 3;
            case 4 -> ry = 4;
            case 5 -> ry = 5;
            case 6 -> ry = 6;
            case 7 -> ry = 7;
        }

        // x 좌표에 대한 switch
        switch ((x + 5500) / 1000) {
            case 0 -> rx = 7;
            case 1 -> rx = 6;
            case 2 -> rx = 5;
            case 3 -> rx = 4;
            case 4 -> rx = 3;
            case 5 -> rx = 2;
            case 6 -> rx = 1;
            case 7 -> rx = 0;
        }

        return new Position(rx, ry);
    }

    private int[] reverseVRcoordinate(Position pos) {
        int x = 0, y = 0;
        int rx = pos.getX();
        int ry = pos.getY();

        // ry에 대한 switch
        switch (ry) {
            case 0 -> y = -3000; // 범위는 -3500 ~ -2500이므로 중간 값으로 설정
            case 1 -> y = -2000; // 범위는 -2500 ~ -1500
            case 2 -> y = -1000; // 범위는 -1500 ~ -500
            case 3 -> y = 0;     // 범위는 -500 ~ 500
            case 4 -> y = 1000;  // 범위는 500 ~ 1500
            case 5 -> y = 2000;  // 범위는 1500 ~ 2500
            case 6 -> y = 3000;  // 범위는 2500 ~ 3500
            case 7 -> y = 4000;  // 범위는 3500 ~ 4500
        }

        // rx에 대한 switch
        switch (rx) {
            case 0 -> x = 2000;  // 범위는 1500 ~ 2500
            case 1 -> x = 1000;  // 범위는 500 ~ 1500
            case 2 -> x = 0;     // 범위는 -500 ~ 500
            case 3 -> x = -1000; // 범위는 -1500 ~ -500
            case 4 -> x = -2000; // 범위는 -2500 ~ -1500
            case 5 -> x = -3000; // 범위는 -3500 ~ -2500
            case 6 -> x = -4000; // 범위는 -4500 ~ -3500
            case 7 -> x = -5000; // 범위는 -5500 ~ -4500
        }

        return new int[] {x, y};
    }


    public void initializeBoard() {

        this.currentPlayer = "BLACK";
        this.currentRole = "COMMANDER";
        this.whiteKingMoved = false;
        this.blackKingMoved = false;
        this.whiteRook1Moved = false;
        this.whiteRook2Moved = false;
        this.blackRook1Moved = false;
        this.blackRook2Moved = false;
        this.enPassantTarget = null;
        this.enPassantTargetColor = null;

        this.moveHistory = new Stack<>();
        this.Webmove = null;
        this.VRmove = null;
        this.checkmated = false;
        this.enpassantMoved = false;
        this.castledRook = null;
        this.promotion = null;

        board = new HashMap<>();

        // Initialize all positions to null
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board.remove(new Position(col, row));
            }
        }

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
