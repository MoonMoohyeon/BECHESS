package com.example.bechess.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WebSocketMessage {
    private Move move;
    private String team;
    private String role;

    public WebSocketMessage() {}

    public WebSocketMessage(Move move, String team, String role) {
        this.move = move;
        this.team = team;
        this.role = role;
    }

}
