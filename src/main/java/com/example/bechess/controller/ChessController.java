package com.example.bechess.controller;

import com.example.bechess.dto.Position;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.bechess.dto.Move;
import com.example.bechess.service.GameState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.util.*;

@Controller
public class ChessController {

    private GameState gameState = new GameState();
    private Set<String> connectedSessions = Collections.synchronizedSet(new HashSet<>());

    private static final Logger log = LoggerFactory.getLogger(ChessController.class);

    private String WebSessionID1 = null;
    private String WebSessionID2 = null;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @MessageMapping("/join")
    public void join(SimpMessageHeaderAccessor headerAccessor) throws IOException {
        String sessionId = headerAccessor.getSessionId();
        if (connectedSessions.size() > 1) {
            log.info("세션 거부됨.");
        } else if (sessionId != null) {
            connectedSessions.add(sessionId);
            log.info("세션 [ {} ] 연결됨", sessionId);

            if (WebSessionID1 == null) {
                WebSessionID1 = sessionId;
            } else if (WebSessionID2 == null) {
                WebSessionID2 = sessionId;
            }

            // 다른 사용자에게 게임 시작 알림 전송
            if (connectedSessions.size() > 1) {
                broadcastConnectedMessage(WebSessionID1);
                broadcastConnectedMessage(WebSessionID2);
                Random random = new Random();
                boolean assignW = random.nextBoolean();
                String color1 = assignW ? "w" : "b";
                String color2 = color1.equals("w") ? "b" : "w";
                messagingTemplate.convertAndSend("/topic/Web", "sessionID : " + WebSessionID1 + " color : " + color1);
                messagingTemplate.convertAndSend("/topic/Web", "sessionID : " + WebSessionID2 + " color : " + color2);
                messagingTemplate.convertAndSend("/topic/Web", "gameStart");
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId.equals(WebSessionID1)) {
            log.info("WebSessionID1 [ {} ] disconnected", sessionId);
            WebSessionID1 = null;  // 세션이 끊어졌으므로 초기화
        } else if (sessionId.equals(WebSessionID2)) {
            log.info("WebSessionID2 [ {} ] disconnected", sessionId);
            WebSessionID2 = null;  // 세션이 끊어졌으므로 초기화
        }

        // 세션 연결 목록에서 제거
        connectedSessions.remove(sessionId);

        // 필요 시 다른 사용자에게 알림
        messagingTemplate.convertAndSend("/topic/Web", "Session [ " + sessionId + " ] disconnected");
    }

    private void broadcastConnectedMessage(String sessionId) {
        messagingTemplate.convertAndSend("/topic/Web", "sessionId\n" + sessionId + "\n");
    }

    private void sendMessageToSpecificSession(String sessionId, String message) {
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/Web", message);
    }


    @MessageMapping("/timeUp") // 클라이언트에서 /app/timeUp로 메시지를 보내면 처리
    public String handleTimeUp(String message) {
        System.out.println("Received time up message: " + message);
        messagingTemplate.convertAndSend("/topic/Web", message);
        messagingTemplate.convertAndSend("/topic/VR", message);
        return message;
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
                messagingTemplate.convertAndSend("/topic/Web", "invalidMove");
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
                messagingTemplate.convertAndSend("/topic/VR", "invalidMove");
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
        messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                " color : " + moveObj.getColor() + " type : " + moveObj.getType() + "\n" + gameState.getBoardState());
        return gameState;
    }

    @MessageMapping("/reset")
    @SendTo("/topic/Web")
    public GameState resetGame() {
        gameState.initializeBoard();
        messagingTemplate.convertAndSend("/topic/Web", "boardReset");
        return gameState;
    }
}
