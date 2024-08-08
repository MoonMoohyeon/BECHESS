package com.example.bechess.controller;

import com.example.bechess.dto.Player;
import com.example.bechess.dto.Position;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.bechess.dto.Move;
import com.example.bechess.service.ChessPiece;
import com.example.bechess.service.GameState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class ChessController {

    private GameState gameState = new GameState();
    private Set<String> connectedSessions = Collections.synchronizedSet(new HashSet<>());

    private static final Logger log = LoggerFactory.getLogger(ChessController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @MessageMapping("/join")
    public void join(SimpMessageHeaderAccessor headerAccessor) throws IOException {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            connectedSessions.add(sessionId);
            log.info("세션 [ {} ] 연결됨", sessionId);
            broadcastConnectedMessage(sessionId);
        }
    }

    private void broadcastConnectedMessage(String sessionId) {
        messagingTemplate.convertAndSend("/topic/message", "세션 [ " + sessionId + " ] 연결되었습니다.");
    }

    @MessageMapping("/move")
    public void handleMove(@Payload Map<String, Object> move) {
        log.info("Move received: eventTime={}, from={}, to={}, player={}",
                move.get("eventTime"), move.get("from"), move.get("to"), move.get("player"));

        try {
            // Extract values from the map and cast to appropriate types
            String eventTime = (String) move.get("eventTime");

            // Extract and convert 'from' position
            String fromJson = objectMapper.writeValueAsString(move.get("from"));
            Position from = objectMapper.readValue(fromJson, Position.class);

            // Extract and convert 'to' position
            String toJson = objectMapper.writeValueAsString(move.get("to"));
            Position to = objectMapper.readValue(toJson, Position.class);

            // Extract and convert 'player' information
            String playerJson = objectMapper.writeValueAsString(move.get("player"));
            Player player = objectMapper.readValue(playerJson, Player.class);

            // Create Move object
            Move moveObj = new Move(eventTime, from, to, player);

            // Log the created Move object for verification
            log.info("Move object created: {}", moveObj);
            log.info("movefrom = {}, {}", moveObj.getFrom().getX(), moveObj.getFrom().getY());

            gameState.processMove(moveObj);
        } catch (Exception e) {
            log.error("Error parsing move message", e);
        }
    }

    @PostMapping("/start")
    public GameState startGame() {
        gameState = new GameState();
        gameState.initializeBoard();
        // Initialize the board and pieces
        return gameState;
    }

    @GetMapping("/state")
    public GameState getGameState() {
        return gameState;
    }

    @MessageMapping("/reset")
    @SendTo("/topic/message")
    public GameState resetGame() {
        gameState.initializeBoard();
        return gameState;
    }
}
