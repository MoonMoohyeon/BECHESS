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