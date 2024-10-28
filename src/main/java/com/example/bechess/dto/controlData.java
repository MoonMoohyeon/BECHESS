package com.example.bechess.dto;

import com.example.bechess.service.GameState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class controlData {
    private GameState gameState = new GameState();

    private int connectedWeb = 0;
    private int connectedVR = 0;

    private String turn = "";
    private String role = "";

    private Move webMove = null;
    private Move vrMove = null;

    private String checkmated = "";
    private String timeover = "";

    private Position promotion = null;
    private Position castle = null;
    private Position enpassant = null;
}
