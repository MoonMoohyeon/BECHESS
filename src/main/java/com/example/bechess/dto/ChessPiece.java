package com.example.bechess.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
public class ChessPiece {
    @Setter
    private String type;
    private String color;
    @Setter
    private Position position;

    public ChessPiece(String type, String color, Position position) {
        this.type = type;
        this.color = color;
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return position.getX() == position.getX() && position.getY() == position.getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(position.getX(), position.getY());
    }
}
