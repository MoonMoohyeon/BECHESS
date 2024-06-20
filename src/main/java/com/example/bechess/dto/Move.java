package com.example.bechess.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Move {
    private Position from;
    private Position to;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

}
