package com.example.bechess.controller;

import com.example.bechess.dto.Position;
import com.example.bechess.dto.controlData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
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
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.util.*;

@Controller
@Getter
@Setter
public class ChessController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ChessController.class);

    private controlData controlData = new controlData();
    private GameState gameState = controlData.getGameState();
    private Set<String> connectedWebSessions = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    public ChessController(controlData controlData) {
        this.controlData = controlData;
    }

    private String WebSessionID1 = null;
    private String WebSessionID2 = null;

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
                controlData.setConnectedWeb(1);
                resetGame();
            } else if (WebSessionID2 == null) {
                WebSessionID2 = sessionId;
                controlData.setConnectedWeb(2);
                resetGame();
            }

//            if (connectedWebSessions.size() == 2 && controlData.getConnectedVR() == 2) {
                messagingTemplate.convertAndSend("/topic/Web", "gameStart");
            resetGame();
//            }
        }
    }

    @MessageMapping("/Web/timeUp")
    public String handleTimeUp(String message) {
        System.out.println("Received time up message: " + message);
        messagingTemplate.convertAndSend("/topic/Web", "gameOver " + message + " lose by timeover");
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
            String special = "";
            Move moveObj = new Move(eventTime, from, to, color, type, special);

            if(!gameState.processMoveWEB(moveObj)) {
                log.info("invalidMove");
                messagingTemplate.convertAndSend("/topic/Web", "invalidMove");
            }
            else {
                gameState = controlData.getGameState();

                VRMessage sendmsg = new VRMessage();
                sendmsg.setHeader("command");

                moveObj = new Move(eventTime, from, to, color, type, special);
                String jsonPayload = objectMapper.writeValueAsString(moveObj);

                if(gameState.isCheckmated()) {
                    messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                            "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                            " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                            " color : " + moveObj.getColor() + " type : " + moveObj.getType() +
                            " gameOver " + gameState.getCurrentPlayer() + "lose by checkmated");

                    special = "gameover";
                    moveObj = new Move(eventTime, from, to, color, type, special);
                    sendmsg.setBody(moveObj);
                    jsonPayload = objectMapper.writeValueAsString(sendmsg);
//                    controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
//                    controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                    log.info("checkmated");
                }
                else if(gameState.getPromotion() != null) {
                    messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                            "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                            " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                            " color : " + moveObj.getColor() + " type : " + moveObj.getType() +
                            " promotion : " + gameState.getPromotion().getX() + "," + gameState.getPromotion().getY());

                    special = "promotion" + gameState.getPromotion().getX() + "," + gameState.getPromotion().getY();
                    moveObj = new Move(eventTime, from, to, color, type, special);
                    sendmsg.setBody(moveObj);
                    jsonPayload = objectMapper.writeValueAsString(sendmsg);
//                    controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
//                    controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                    log.info("promotion");
                    gameState.setPromotion(null);
                }
                else if(gameState.getCastledRook() != null) {
                    messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                            "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                            " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                            " color : " + moveObj.getColor() + " type : " + moveObj.getType() +
                            " castle : " + gameState.getCastledRook().getX() + "," + gameState.getCastledRook().getY());

                    special = "castle" + gameState.getCastledRook().getX() + "," + gameState.getCastledRook().getY();
                    moveObj = new Move(eventTime, from, to, color, type, special);
                    sendmsg.setBody(moveObj);
                    jsonPayload = objectMapper.writeValueAsString(sendmsg);
//                    controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
//                    controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                    log.info("castledRook");
                    gameState.setCastledRook(null);
                }
                else if(gameState.isEnpassantMoved()) {
                    messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                            "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                            " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                            " color : " + moveObj.getColor() + " type : " + moveObj.getType() +
                            " enpassant : " + gameState.getEnPassantTarget().getX() + "," + gameState.getEnPassantTarget().getY());

                    special = "enpassant" + gameState.getEnPassantTarget().getX() + "," + gameState;
                    moveObj = new Move(eventTime, from, to, color, type, special);
                    sendmsg.setBody(moveObj);
                    jsonPayload = objectMapper.writeValueAsString(sendmsg);
//                    controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
//                    controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                    log.info("enpassanttarget");
                    gameState.setEnpassantMoved(false);
                }
                else {
                    messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                            "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                            " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                            " color : " + moveObj.getColor() + " type : " + moveObj.getType());
//                + "\n" + gameState.getBoardState());

                    sendmsg.setBody(moveObj);
                    jsonPayload = objectMapper.writeValueAsString(sendmsg);
//                    controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
//                    controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                    log.info("else");
                }
            }

        } catch (Exception e) {
            log.error("Error parsing move message", e);
        }
    }

//    @MessageMapping("/Web/promotion")
//    public String handlePromotion(String message) {
//        System.out.println("Promotion : " + message);
//
//        // "to :" 위치를 찾고 좌표 부분을 추출
//        // "to : " 이후의 부분을 추출
//        int toIndex = message.indexOf("to : ");
//        String numbersPart = message.substring(toIndex + 5); // "1,7\npromotion : r" 부분
//
//// ","로 분리하여 각 숫자 추출
//        String[] numbers = numbersPart.split(",|\n"); // 쉼표나 줄바꿈 기준으로 나누기
//        int toX = Integer.parseInt(numbers[0].trim()); // 첫 번째 숫자
//        int toY = Integer.parseInt(numbers[1].trim()); // 두 번째 숫자
//        char promotion = message.charAt(message.length() - 1);
//
//        Position promotionPosition = new Position(toX, toY);
//
//        log.info("promotion : " + promotion + toX + ", " + toY);
//
//        gameState.processPromotion(promotionPosition, promotion);
//
//        return message;
//    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Web disconnect
        if (connectedWebSessions.contains(sessionId)) {
            if (sessionId.equals(WebSessionID1)) {
                log.info("WebSessionID1 [ {} ] disconnected", sessionId);
                controlData.setConnectedWeb(controlData.getConnectedWeb() - 1);
                WebSessionID1 = null;
            } else if (sessionId.equals(WebSessionID2)) {
                log.info("WebSessionID2 [ {} ] disconnected", sessionId);
                controlData.setConnectedWeb(controlData.getConnectedWeb() - 1);
                WebSessionID2 = null;
            }
            connectedWebSessions.remove(sessionId);
            messagingTemplate.convertAndSend("/topic/Web", "Web Session [ " + sessionId + " ] disconnected");
        }
    }

    @MessageMapping("/Web/reset")
    @SendTo("/topic/Web")
    public GameState resetGame() {
        gameState.initializeBoard();
        messagingTemplate.convertAndSend("/topic/Web", "boardReset");
        return gameState;
    }
}
