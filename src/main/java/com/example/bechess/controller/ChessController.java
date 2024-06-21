package com.example.bechess.controller;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.WebSocketMessage;
import com.example.bechess.service.ChessPiece;
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
        Move move = message.getMove();
        // Validate move and update gameState
        // Assuming you have logic to handle the move here
        ChessPiece piece = gameState.getBoard().get(move.getFrom());
        if (message.getTeam().equals(gameState.getCurrentTeam()) &&
                message.getRole().equals(gameState.getCurrentRole()) &&
                gameState.isValidMove(message.getMove())) {
            gameState.getBoard().put(move.getTo(), piece);
            gameState.getBoard().remove(move.getFrom());
            piece.setPosition(move.getTo());
            gameState.switchPlayer();
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