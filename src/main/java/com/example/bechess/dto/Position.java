package com.example.bechess.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Position {
    private int x;
    private int y;

    public static Position fromString(String positionString) {
        String[] parts = positionString.split(",");
        return new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
