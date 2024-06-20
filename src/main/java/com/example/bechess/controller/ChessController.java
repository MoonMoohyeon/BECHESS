package com.example.bechess.controller;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.WebSocketMessage;
import com.example.bechess.service.GameState;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChessController {

    private GameState gameState = new GameState();

    @MessageMapping("/move")
    @SendTo("/topic/gameState")
    public GameState makeMove(WebSocketMessage message) throws Exception {
        if (message.getTeam().equals(gameState.getCurrentTeam()) &&
                message.getRole().equals(gameState.getCurrentRole()) &&
                gameState.isValidMove(message.getMove())) {
            gameState.updateBoard(message.getMove());
            gameState.switchTurn();
        }
        return gameState;
    }

    @MessageMapping("/reset")
    @SendTo("/topic/gameState")
    public GameState resetGame() {
        gameState = new GameState();
        return gameState;
    }
}