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
        this.currentRole = "COMMANDER";
         initializeBoard();
    }

    public boolean processMoveWEB(Move move) {
        printBoard();
        log.info(String.valueOf(isValidMove(move, board)));
        if (isValidMove(move, board)) {
            updateBoard(move);
            switchPlayer();
            switchTurn();
            log.info("현재 정보 : ", getCurrentPlayer(), getCurrentRole());
            printBoard();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean processMoveVR(Move move) {
        printBoard();
        log.info(String.valueOf(isValidMove(move, board)));
        if (isValidMove(move, board)) {
            updateBoard(move);
            switchPlayer();
            switchTurn();
            log.info("현재 정보 : ", getCurrentPlayer(), getCurrentRole());
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

    public boolean isValidMove(Move move, Map<Position, ChessPiece> board) {
        Position from = move.getFrom();
        Position to = move.getTo();
        ChessPiece piece = board.get(from);

        if (piece == null || !piece.getColor().equals(currentPlayer)) {
            return false; // 기물이 없거나 상대 기물인 경우
        }

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
                return isValidKingMove(move, board); // 이미 리팩토링된 메서드 사용
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

        // Normal king move (one square in any direction)
        if (dx <= 1 && dy <= 1) {
            return !isPositionUnderAttack(to, board);  // 새로운 위치가 공격받지 않는지 확인
        }

        // Castling
        if (dx == 2 && dy == 0) {
            if (getCurrentPlayer().equals("WHITE") && !whiteKingMoved &&
                    ((to.getX() == 5 && !whiteRook2Moved && isPathClear(from, new Position(7, 0), board) && !isInCheckOrThroughCheck(from, new Position(6, 0), board)) ||
                            (to.getX() == 1 && !whiteRook1Moved && isPathClear(from, new Position(0, 0), board) && !isInCheckOrThroughCheck(from, new Position(2, 0), board)))) {
                return true;
            }
            if (getCurrentPlayer().equals("BLACK") && !blackKingMoved &&
                    ((to.getX() == 5 && !blackRook2Moved && isPathClear(from, new Position(7, 7), board) && !isInCheckOrThroughCheck(from, new Position(6, 7), board)) ||
                            (to.getX() == 1 && !blackRook1Moved && isPathClear(from, new Position(0, 7), board) && !isInCheckOrThroughCheck(from, new Position(2, 7), board)))) {
                return true;
            }
        }
        return false;
    }

    public boolean isPositionUnderAttack(Position position, Map<Position, ChessPiece> board) {
        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();

            // Only consider the opponent's pieces
            if (!piece.getColor().equals(getCurrentPlayer())) {
                Move potentialMove = new Move(entry.getKey(), position);
                if (isValidMove(potentialMove, board)) {
                    return true; // The position is under attack by an opponent's piece
                }
            }
        }
        return false;
    }

    private boolean isInCheckOrThroughCheck(Position from, Position to, Map<Position, ChessPiece> board) {
        // Check the final position first (to)
        if (isPositionUnderAttack(to, board)) {
            return true;
        }

        // For castling, check if any position between `from` and `to` is under attack
        if (Math.abs(to.getX() - from.getX()) == 2) { // Castling case
            int step = (to.getX() - from.getX()) / 2; // Direction of castling
            Position midPosition = new Position(from.getX() + step, from.getY());

            // Check if the intermediate position is under attack
            if (isPositionUnderAttack(midPosition, board)) {
                return true;
            }
        }

        return false;
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
        // 현재 플레이어의 킹의 위치를 찾음
        Position kingPosition = findKingPosition(currentPlayer);

        // 킹이 체크 상태인지 확인
        if (!isPositionUnderAttack(kingPosition, board)) {
            return false;  // 킹이 체크 상태가 아니라면 체크메이트가 아님
        }

        // 킹을 체크 상태에서 벗어날 수 있는지 확인
        if (canKingEscapeCheck(kingPosition)) {
            return false;  // 킹이 체크 상태에서 벗어날 수 있다면 체크메이트가 아님
        }

        // 다른 기물이 체크를 막을 수 있는지 확인
        if (canBlockOrCaptureAttacker(kingPosition)) {
            return false;  // 공격자를 제거하거나 막을 수 있다면 체크메이트가 아님
        }

        // 위의 방법들로 체크를 피할 수 없다면 체크메이트
        return true;
    }

    // 킹의 위치 찾기
    private Position findKingPosition(String color) {
        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();
            if (piece.getType().equals("KING") && piece.getColor().equals(color)) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("킹이 존재하지 않습니다.");  // 킹이 반드시 존재해야 함
    }

    // 킹이 체크 상태에서 벗어날 수 있는지 확인 (킹이 이동할 수 있는 모든 위치를 확인)
    private boolean canKingEscapeCheck(Position kingPosition) {
        ChessPiece king = board.get(kingPosition);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;  // 제자리 움직임 무시
                Position newPosition = new Position(kingPosition.getX() + dx, kingPosition.getY() + dy);
                if (isValidKingMove(new Move(kingPosition, newPosition), board) && !isPositionUnderAttack(newPosition, board)) {
                    return true;  // 킹이 체크 상태에서 벗어날 수 있다면 true 반환
                }
            }
        }
        return false;  // 킹이 체크 상태에서 벗어날 수 없다면 false 반환
    }

    // 다른 기물이 공격자를 막거나 제거할 수 있는지 확인
    private boolean canBlockOrCaptureAttacker(Position kingPosition) {
        // 상대방 기물이 킹을 공격하고 있는지 확인
        for (Map.Entry<Position, ChessPiece> entry : board.entrySet()) {
            ChessPiece attacker = entry.getValue();
            if (!attacker.getColor().equals(currentPlayer)) {  // 공격자는 상대방 기물이어야 함
                Move potentialMove = new Move(entry.getKey(), kingPosition);
                if (isValidMove(potentialMove, board)) {
                    // 공격자를 제거하거나 경로를 막을 수 있는지 확인
                    for (Map.Entry<Position, ChessPiece> defenderEntry : board.entrySet()) {
                        ChessPiece defender = defenderEntry.getValue();
                        if (defender.getColor().equals(currentPlayer)) {  // 방어자는 현재 플레이어의 기물이어야 함
                            Move blockOrCaptureMove = new Move(defenderEntry.getKey(), attacker.getPosition());
                            if (isValidMove(blockOrCaptureMove, board)) {
                                return true;  // 공격자를 제거하거나 막을 수 있으면 true 반환
                            }
                        }
                    }
                }
            }
        }
        return false;  // 공격자를 막거나 제거할 수 없다면 false 반환
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

        // 기물 잡기 처리
        if (board.containsKey(move.getTo()) && board.get(move.getTo()) != piece) {
            board.remove(move.getTo());
        }

        // 앙파상으로 상대 폰을 잡은 경우, 해당 폰을 제거합니다.
        if (piece.getType().equals("PAWN") && move.getTo().equals(enPassantTarget)) {
            Position capturePosition = new Position(move.getTo().getX(), move.getFrom().getY());
            board.remove(capturePosition);  // 앙파상으로 잡힌 폰을 제거
        }

        // 새로운 위치에 기물을 배치합니다.
        board.put(move.getTo(), piece);

        log.info("piece = {}, {}, {}, {}", piece.getPosition().getX(), piece.getPosition().getY(), piece.getType(), piece.getColor());

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

    private Position VRcoordinate(int x, int y) {
        int rx = 0, ry = 0;

        // y 좌표에 대한 switch
        switch((y + 3500) / 1000) {
            case 0:
                ry = 0;
                break;
            case 1:
                ry = 1;
                break;
            case 2:
                ry = 2;
                break;
            case 3:
                ry = 3;
                break;
            case 4:
                ry = 4;
                break;
            case 5:
                ry = 5;
                break;
            case 6:
                ry = 6;
                break;
            case 7:
                ry = 7;
                break;
        }

        // x 좌표에 대한 switch
        switch((x + 5500) / 1000) {
            case 0:
                rx = 7;
                break;
            case 1:
                rx = 6;
                break;
            case 2:
                rx = 5;
                break;
            case 3:
                rx = 4;
                break;
            case 4:
                rx = 3;
                break;
            case 5:
                rx = 2;
                break;
            case 6:
                rx = 1;
                break;
            case 7:
                rx = 0;
                break;
        }

        return new Position(rx, ry);
    }

    private int[] reverseVRcoordinate(Position pos) {
        int x = 0, y = 0;
        int rx = pos.getX();
        int ry = pos.getY();

        // ry에 대한 switch
        switch (ry) {
            case 0:
                y = -3000; // 범위는 -3500 ~ -2500이므로 중간 값으로 설정
                break;
            case 1:
                y = -2000; // 범위는 -2500 ~ -1500
                break;
            case 2:
                y = -1000; // 범위는 -1500 ~ -500
                break;
            case 3:
                y = 0;     // 범위는 -500 ~ 500
                break;
            case 4:
                y = 1000;  // 범위는 500 ~ 1500
                break;
            case 5:
                y = 2000;  // 범위는 1500 ~ 2500
                break;
            case 6:
                y = 3000;  // 범위는 2500 ~ 3500
                break;
            case 7:
                y = 4000;  // 범위는 3500 ~ 4500
                break;
        }

        // rx에 대한 switch
        switch (rx) {
            case 0:
                x = 2000;  // 범위는 1500 ~ 2500
                break;
            case 1:
                x = 1000;  // 범위는 500 ~ 1500
                break;
            case 2:
                x = 0;     // 범위는 -500 ~ 500
                break;
            case 3:
                x = -1000; // 범위는 -1500 ~ -500
                break;
            case 4:
                x = -2000; // 범위는 -2500 ~ -1500
                break;
            case 5:
                x = -3000; // 범위는 -3500 ~ -2500
                break;
            case 6:
                x = -4000; // 범위는 -4500 ~ -3500
                break;
            case 7:
                x = -5000; // 범위는 -5500 ~ -4500
                break;
        }

        return new int[] {x, y};
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
