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

    private String color;
    private String type;

    public Move(String eventTime, Position from, Position to, String color, String type) {
        this.eventTime = eventTime;
        this.from = from;
        this.to = to;
        this.color = color;
        this.type = type;
    }

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }
}
