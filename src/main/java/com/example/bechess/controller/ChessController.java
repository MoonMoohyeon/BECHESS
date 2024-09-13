package com.example.bechess.controller;

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

            if (connectedSessions.size() > 1) {
                messagingTemplate.convertAndSend("/topic/message", "gameStart");
            }
        }
    }

    private void broadcastConnectedMessage(String sessionId) {
        messagingTemplate.convertAndSend("/topic/message", "세션 [ " + sessionId + " ] 연결되었습니다.");
    }

    @MessageMapping("/timeUp") // 클라이언트에서 /app/timeUp로 메시지를 보내면 처리
    @SendTo("/topic/timeUp") // 서버에서 /topic/timeUp로 메시지를 전송
    public String handleTimeUp(String message) {
        System.out.println("Received time up message: " + message);
        return "Time is up!";
    }

    @MessageMapping("/moveWEB")
    public void handleMoveWEB(@Payload Map<String, Object> move) {
        log.info("Move received: eventTime={}, from={}, to={}, color={}",
                move.get("eventTime"), move.get("from"), move.get("to"), move.get("color"));

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
            String color = (String) move.get("color");

            // Extract and convert 'type' information
            String type = (String) move.get("type");

            // Create Move object
            Move moveObj = new Move(eventTime, from, to, color, type);

            // Log the created Move object for verification
            log.info("Move object created: {}", moveObj);
            log.info("movefrom = {}, {}", moveObj.getFrom().getX(), moveObj.getFrom().getY());

            if(!gameState.processMoveWEB(moveObj)) {
                log.info("invalidMove");
                messagingTemplate.convertAndSend("/topic/message", "invalidMove");
            }
            else {
                getGameState(moveObj);
            }

        } catch (Exception e) {
            log.error("Error parsing move message", e);
        }
    }

    @MessageMapping("/moveVR")
    public void handleMoveVR(@Payload Map<String, Object> move) {
        log.info("Move received: eventTime={}, from={}, to={}, color={}",
                move.get("eventTime"), move.get("from"), move.get("to"), move.get("color"));

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
            String color = (String) (move.get("color"));

            // Extract and convert 'type' information
            String type = (String) move.get("type");

            // Create Move object
            Move moveObj = new Move(eventTime, from, to, color, type);

            // Log the created Move object for verification
            log.info("Move object created: {}", moveObj);
            log.info("movefrom = {}, {}", moveObj.getFrom().getX(), moveObj.getFrom().getY());

            if(!gameState.processMoveVR(moveObj)) {
                log.info("invalidMove");
                messagingTemplate.convertAndSend("/topic/message", "invalidMove");
            }
            else {
                getGameState(moveObj);
            }

        } catch (Exception e) {
            log.error("Error parsing move message", e);
        }
    }

    @GetMapping("/state")
    public GameState getGameState(Move moveObj) {
        messagingTemplate.convertAndSend("/topic/message", "validMove\n" +
                "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                " color : " + moveObj.getColor() + " type : " + moveObj.getType() + "\n" + gameState.getBoardState());
        return gameState;
    }

    @MessageMapping("/reset")
    @SendTo("/topic/message")
    public GameState resetGame() {
        gameState.initializeBoard();
        messagingTemplate.convertAndSend("/topic/message", "boardReset");
        return gameState;
    }
}
