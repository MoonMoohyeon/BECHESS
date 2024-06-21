package com.example.bechess.controller;

import com.example.bechess.dto.*;
import com.example.bechess.service.GameState;
import org.springframework.web.bind.annotation.*;

@RestController
public class GameController {
    private GameState gameState = new GameState();

    @PostMapping("/start")
    public GameState startGame() {
        gameState = new GameState();
        // Initialize the board and pieces
        return gameState;
    }

    @GetMapping("/state")
    public GameState getGameState() {
        return gameState;
    }
}
