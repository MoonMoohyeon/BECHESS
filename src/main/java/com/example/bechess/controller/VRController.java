package com.example.bechess.controller;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.Position;
import com.example.bechess.dto.controlData;
import com.example.bechess.service.GameState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Getter
@Setter
public class VRController extends TextWebSocketHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private List<WebSocketSession> connectedVRSessions = Collections.synchronizedList(new ArrayList<>());
    private static final Logger log = LoggerFactory.getLogger(ChessController.class);

    private controlData controlData = new controlData();
    private GameState gameState = controlData.getGameState();

    @Autowired
    public VRController(controlData controlData) {
        this.controlData = controlData;
    }

    private WebSocketSession VRSessionID1 = null;
    private WebSocketSession VRSessionID2 = null;

    // 웹소켓 연결이 열릴 때 호출
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if(connectedVRSessions.size() > 1) {
            log.info("세션 거부됨.");
        }
        else if(VRSessionID1 == null) {
            VRSessionID1 = session;
            connectedVRSessions.add(session);
            controlData.setConnectedVR(1);
        }
        else if(VRSessionID2 == null) {
            VRSessionID2 = session;
            connectedVRSessions.add(session);
            controlData.setConnectedVR(2);
        }

//        if(connectedVRSessions.size() == 2 && controlData.getConnectedWeb() == 2) {
            connectedVRSessions.get(0).sendMessage(new TextMessage("gameStart"));
            connectedVRSessions.get(1).sendMessage(new TextMessage("gameStart"));
//        }

        System.out.println("New WebSocket connection established: " + session.getId());
        session.sendMessage(new TextMessage("Welcome! Connection established."));
    }

    // 클라이언트로부터 메시지 수신 시 호출
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        gameState = controlData.getGameState();
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("Received message: " + message.getPayload());
        session.sendMessage(new TextMessage("Echo: " + message.getPayload()));

        try {
            String payload = message.getPayload();
            Move moveData = objectMapper.readValue(payload, Move.class);

            // 메시지를 JSON으로 파싱
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            System.out.println("Received JSON message: " + jsonNode.toString());

            if (jsonNode.findValue("move") != null) {
                log.info("Move received: from={}, to={}, color={}, type={}, battle={}",
                        jsonNode.get("from"), jsonNode.get("to"), jsonNode.get("color"), jsonNode.get("type"), jsonNode.get("battle"));

                String fromJson = objectMapper.writeValueAsString(jsonNode.get("from"));
                Position from = objectMapper.readValue(fromJson, Position.class);
                String toJson = objectMapper.writeValueAsString(jsonNode.get("to"));
                Position to = objectMapper.readValue(toJson, Position.class);
                String color = String.valueOf(jsonNode.get("color"));
                String type = String.valueOf(jsonNode.get("type"));
                String battle = String.valueOf(jsonNode.get("battle"));
                String special = "";

                // Create Move object
                Move moveObj = new Move(from, to, color, type);

                if (!gameState.processMoveVR(moveObj)) {
                    log.info("invalidMove");
                    session.sendMessage(new TextMessage("invalidMove"));
                } else {
                    log.info("validMove");

                    messagingTemplate.convertAndSend("/topic/Web", "validMove\n" +
                            "from : " + moveObj.getFrom().getX() + "," + moveObj.getFrom().getY() +
                            " to : " + moveObj.getTo().getX() + "," + moveObj.getTo().getY() +
                            " color : " + moveObj.getColor() + " type : " + moveObj.getType());

                    connectedVRSessions.get(0).sendMessage(new TextMessage("validMove"));
                    connectedVRSessions.get(1).sendMessage(new TextMessage("validMove"));

                    if (gameState.isCheckmated()) {
                        messagingTemplate.convertAndSend("topic/Web", "gameOver " + gameState.getCurrentPlayer() + "lose by checkmated");

                        special = "gameover";
                        moveObj = new Move(from, to, color, type, special);
                        String jsonPayload = objectMapper.writeValueAsString(moveObj);
                        controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
                        controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

//                        connectedVRSessions.get(0).sendMessage(new TextMessage("gameOver" + gameState.getCurrentPlayer() + "lose by checkmate"));
//                        connectedVRSessions.get(1).sendMessage(new TextMessage("gameOver" + gameState.getCurrentPlayer() + "lose by checkmate"));
                        gameState.setCheckmated(false);
                    } else if (gameState.getPromotion() != null) {
                        messagingTemplate.convertAndSend("topic/Web", "promotion : " + gameState.getPromotion().getX() + "," + gameState.getPromotion().getY());

                        special = "promotion" + gameState.getPromotion().getX() + "," + gameState.getPromotion().getY();
                        moveObj = new Move(from, to, color, type, special);
                        String jsonPayload = objectMapper.writeValueAsString(moveObj);
                        controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
                        controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                        connectedVRSessions.get(0).sendMessage(new TextMessage("promotion : " + gameState.getPromotion().getX() + "," + gameState.getPromotion().getY()));
                        connectedVRSessions.get(1).sendMessage(new TextMessage("promotion : " + gameState.getPromotion().getX() + "," + gameState.getPromotion().getY()));
                        gameState.setPromotion(null);
                    } else if (gameState.getCastledRook() != null) {
                        messagingTemplate.convertAndSend("topic/Web", "castle : " + gameState.getCastledRook().getX() + "," + gameState.getCastledRook().getY());

                        special = "castle" + gameState.getCastledRook().getX() + "," + gameState.getCastledRook().getY();
                        moveObj = new Move(from, to, color, type, special);
                        String jsonPayload = objectMapper.writeValueAsString(moveObj);
                        controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
                        controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                        connectedVRSessions.get(0).sendMessage(new TextMessage("castle :  " + gameState.getCastledRook().getX() + "," + gameState.getCastledRook().getY()));
                        connectedVRSessions.get(1).sendMessage(new TextMessage("castle : " + gameState.getCastledRook().getX() + "," + gameState.getCastledRook().getY()));
                        gameState.setCastledRook(null);
                    } else if (gameState.getEnPassantTarget() != null) {
                        messagingTemplate.convertAndSend("topic/Web", "enpassant : " + gameState.getEnPassantTarget().getX() + "," + gameState.getEnPassantTarget().getY());

                        special = "enpassant" + gameState.getEnPassantTarget().getX() + "," + gameState.getEnPassantTarget().getY();
                        moveObj = new Move(from, to, color, type, special);
                        String jsonPayload = objectMapper.writeValueAsString(moveObj);
                        controlData.getConnectedVRSessions().get(0).sendMessage(new TextMessage(jsonPayload));
                        controlData.getConnectedVRSessions().get(1).sendMessage(new TextMessage(jsonPayload));

                        connectedVRSessions.get(0).sendMessage(new TextMessage("enpassant : " + gameState.getEnPassantTarget().getX() + "," + gameState.getEnPassantTarget().getY()));
                        connectedVRSessions.get(1).sendMessage(new TextMessage("enpassant : " + gameState.getEnPassantTarget().getX() + "," + gameState.getEnPassantTarget().getY()));
                        gameState.setEnPassantTarget(null);
                    }
                }
            } else if (jsonNode.findValue("battle") != null) {

            }

            JsonNode responseNode = objectMapper.createObjectNode()
                    .put("response", "Echo")
                    .set("original", jsonNode);

            String jsonResponse = objectMapper.writeValueAsString(responseNode);
            session.sendMessage(new TextMessage(jsonResponse));
        } catch (Exception e) {
            log.error("Error parsing move message", e);
        }
    }

    // 웹소켓 연결이 닫힐 때 호출
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        session.close();
        if(session.getId().equals(VRSessionID1.getId())) {
            VRSessionID1 = null;
            connectedVRSessions.remove(session);
        }
        else if(session.getId().equals(VRSessionID2.getId())) {
            VRSessionID2 = null;
            connectedVRSessions.remove(session);
        }
        System.out.println("WebSocket connection closed: " + session.getId());
    }
}
