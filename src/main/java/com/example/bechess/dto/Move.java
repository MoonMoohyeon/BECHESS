package com.example.bechess.dto;

import com.example.bechess.service.ChessPiece;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Move {
    private String eventTime;

    @JsonDeserialize(using = PositionDeserializer.class)
    private Position from;

    @JsonDeserialize(using = PositionDeserializer.class)
    private Position to;

    private Player player;

    public Move(String eventTime, Position from, Position to, Player player) {
        this.eventTime = eventTime;
        this.from = from;
        this.to = to;
        this.player = player;
    }

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }
}
