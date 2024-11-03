package com.example.bechess.dto;

import com.example.bechess.service.GameState;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;

@Component
@Getter
@Setter
public class controlData {
    private GameState gameState = new GameState();

    private int connectedWeb = 0;
    private int connectedVR = 0;

    private Set<String> connectedWebSessions = Collections.synchronizedSet(new HashSet<>());
    private List<WebSocketSession> connectedVRSessions = Collections.synchronizedList(new ArrayList<>());

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
