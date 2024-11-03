package com.example.bechess.controller;

import com.example.bechess.dto.Move;
import com.example.bechess.dto.Position;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

// MoveData 클래스는 이전에 정의한 대로 유지합니다.

public class VRMessage {
    private String header;
    private Move body;

    // Getters and Setters
    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Move getBody() {
        return body;
    }

    public void setBody(Move body) {
        this.body = body;
    }
}

public class ChessWebSocketClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSession session;

    public ChessWebSocketClient(WebSocketSession session) {
        this.session = session;
    }

    public void sendCommandMessage(String header, Position from, Position to, String color, String type, String special) throws Exception {
        // MoveData 객체 생성
        Move moveData = null;
        moveData.setFrom(from);
        moveData.setTo(to);
        moveData.setColor(color);
        moveData.setType(type);

        // CommandMessage 객체 생성 및 설정
        VRMessage commandMessage = new VRMessage();
        commandMessage.setHeader(header);
        commandMessage.setBody(moveData);

        // CommandMessage 객체를 JSON 문자열로 변환
        String jsonPayload = objectMapper.writeValueAsString(commandMessage);

        // WebSocket 메시지로 전송
        TextMessage message = new TextMessage(jsonPayload);
        session.sendMessage(message);
    }
}
