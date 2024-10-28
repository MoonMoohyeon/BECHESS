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

    private com.example.bechess.dto.controlData controlData = new controlData();
    private GameState gameState = controlData.getGameState();

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
        ObjectMapper objectMapper = new ObjectMapper();

        // 메시지를 JSON으로 파싱
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        System.out.println("Received JSON message: " + jsonNode.toString());

        if(jsonNode.findValue("move") != null) {
            log.info("Move received: eventTime={}, from={}, to={}, color={}",
                    jsonNode.get("eventTime"), jsonNode.get("from"), jsonNode.get("to"), jsonNode.get("color"));

            String eventTime = String.valueOf(jsonNode.get("eventTime"));
            String fromJson = objectMapper.writeValueAsString(jsonNode.get("from"));
            Position from = objectMapper.readValue(fromJson, Position.class);
            String toJson = objectMapper.writeValueAsString(jsonNode.get("to"));
            Position to = objectMapper.readValue(toJson, Position.class);
            String color = String.valueOf(jsonNode.get("color"));
            String type = String.valueOf(jsonNode.get("type"));

            // Create Move object
            Move moveObj = new Move(eventTime, from, to, color, type);

            if(!gameState.processMoveVR(moveObj)) {
                log.info("invalidMove");
                session.sendMessage(new TextMessage("invalidMove"));
            }
            else {
                log.info("validMove");
                session.sendMessage(new TextMessage("validMove"));
            }
        }
        else if(jsonNode.findValue("battle") != null) {

        }

        // 응답 JSON 작성
        JsonNode responseNode = objectMapper.createObjectNode()
                .put("response", "Echo")
                .set("original", jsonNode);

        // JSON 응답을 문자열로 변환하여 전송
        String jsonResponse = objectMapper.writeValueAsString(responseNode);
        session.sendMessage(new TextMessage(jsonResponse));
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
