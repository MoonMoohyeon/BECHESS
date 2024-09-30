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
    private Set<String> connectedWebSessions = Collections.synchronizedSet(new HashSet<>());
    private Set<String> connectedVRSessions = Collections.synchronizedSet(new HashSet<>());

    private static final Logger log = LoggerFactory.getLogger(ChessController.class);


    private String WebSessionID1 = null;
    private String WebSessionID2 = null;
    private String VRSessionID1 = null;
    private String VRSessionID2 = null;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @MessageMapping("/Web/join")
    public void Webjoin(SimpMessageHeaderAccessor headerAccessor) throws IOException {
        String sessionId = headerAccessor.getSessionId();
        if (connectedWebSessions.size() > 1) {
            log.info("세션 거부됨.");
        } else if (sessionId != null) {
            connectedWebSessions.add(sessionId);
            log.info("세션 [ {} ] 연결됨", sessionId);

            if (WebSessionID1 == null) {
                WebSessionID1 = sessionId;
            } else if (WebSessionID2 == null) {
                WebSessionID2 = sessionId;
            }

            if (connectedWebSessions.size() == 2) {
                assignColorsAndStartGame("Web", WebSessionID1, WebSessionID2);
            }
        }
    }

    @MessageMapping("/VR/join")
    public void VRjoin(SimpMessageHeaderAccessor headerAccessor) throws IOException {
        String sessionId = headerAccessor.getSessionId();
        if (connectedVRSessions.size() > 1) {
            log.info("세션 거부됨.");
        } else if (sessionId != null) {
            connectedVRSessions.add(sessionId);
            log.info("세션 [ {} ] 연결됨", sessionId);

            if (VRSessionID1 == null) {
                VRSessionID1 = sessionId;
            } else if (VRSessionID2 == null) {
                VRSessionID2 = sessionId;
            }

            if (connectedVRSessions.size() == 2) {
                assignColorsAndStartGame("VR", VRSessionID1, VRSessionID2);
            }
        }
    }

    @MessageMapping("/Web/timeUp") // 클라이언트에서 /app/timeUp로 메시지를 보내면 처리
    public String handleTimeUp(String message) {
        System.out.println("Received time up message: " + message);
        messagingTemplate.convertAndSend("/topic/Web", message);
        messagingTemplate.convertAndSend("/topic/VR", message);
        return message;
    }

    @MessageMapping("/Web/move")
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

    @MessageMapping("/VR/move")
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

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Web disconnect
        if (connectedWebSessions.contains(sessionId)) {
            if (sessionId.equals(WebSessionID1)) {
                log.info("WebSessionID1 [ {} ] disconnected", sessionId);
                WebSessionID1 = null;
            } else if (sessionId.equals(WebSessionID2)) {
                log.info("WebSessionID2 [ {} ] disconnected", sessionId);
                WebSessionID2 = null;
            }
            connectedWebSessions.remove(sessionId);
            messagingTemplate.convertAndSend("/topic/Web", "Web Session [ " + sessionId + " ] disconnected");
        }

        // VR disconnect
        if (connectedVRSessions.contains(sessionId)) {
            if (sessionId.equals(VRSessionID1)) {
                log.info("VRSessionID1 [ {} ] disconnected", sessionId);
                VRSessionID1 = null;
            } else if (sessionId.equals(VRSessionID2)) {
                log.info("VRSessionID2 [ {} ] disconnected", sessionId);
                VRSessionID2 = null;
            }
            connectedVRSessions.remove(sessionId);
            messagingTemplate.convertAndSend("/topic/VR", "VR Session [ " + sessionId + " ] disconnected");
        }
    }

    private void broadcastConnectedMessage(String sessionId) {
        messagingTemplate.convertAndSend("/topic/Web", "sessionId\n" + sessionId + "\n");
    }

    private void sendMessageToSpecificSession(String sessionId, String message) {
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/Web", message);
    }

    @GetMapping("/state")
    public GameState getGameState(Move moveObj) {
        messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                " color : " + moveObj.getColor() + " type : " + moveObj.getType() + "\n" + gameState.getBoardState());
        return gameState;
    }

    @MessageMapping("/Web/reset")
    @SendTo("/topic/Web")
    public GameState resetGame() {
        gameState = new GameState();
        gameState.initializeBoard();
        messagingTemplate.convertAndSend("/topic/Web", "boardReset");
        return gameState;
    }

    private void assignColorsAndStartGame(String platform, String sessionID1, String sessionID2) {
        Random random = new Random();
        boolean assignW = random.nextBoolean();
        String color1 = assignW ? "w" : "b";
        String color2 = color1.equals("w") ? "b" : "w";
        messagingTemplate.convertAndSend("/topic/" + platform, "sessionID : " + sessionID1 + " color : " + color1);
        messagingTemplate.convertAndSend("/topic/" + platform, "sessionID : " + sessionID2 + " color : " + color2);
        messagingTemplate.convertAndSend("/topic/" + platform, "gameStart");
    }
}
