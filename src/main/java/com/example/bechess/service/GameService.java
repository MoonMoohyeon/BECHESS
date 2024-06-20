package com.example.bechess.service;

import com.example.bechess.dto.Move;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private GameState gameState = new GameState();

    public GameState processMove(Move move) {
        // 말의 유효성 검증 및 이동 처리
        if (gameState.isValidMove(move)) {
            gameState.updateBoard(move);
            gameState.switchPlayer();
        }
        return gameState;
    }
}
