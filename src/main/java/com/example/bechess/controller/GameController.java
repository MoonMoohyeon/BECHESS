//package com.example.bechess.controller;
//
//import com.example.bechess.dto.Move;
//import com.example.bechess.service.GameService;
//import com.example.bechess.service.GameState;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
//import org.springframework.stereotype.Controller;
//
//@Controller
//public class GameController {
//
//    @Autowired
//    private GameService gameService;
//
//    @MessageMapping("/move")
//    @SendTo("/topic/game")
//    public GameState makeMove(Move move) throws Exception {
//        return gameService.processMove(move);
//    }
//}
