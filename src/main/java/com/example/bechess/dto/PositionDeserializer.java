package com.example.bechess.dto;

import com.example.bechess.dto.Position;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

public class PositionDeserializer extends JsonDeserializer<Position> {

    @Override
    public Position deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String positionString = p.getText();
        return Position.fromString(positionString);
    }
}
