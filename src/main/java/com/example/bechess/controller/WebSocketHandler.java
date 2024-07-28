//package com.example.bechess.controller;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//@Component
//public class WebSocketHandler extends TextWebSocketHandler {
//    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);
//    private List<WebSocketSession> sessionList = new CopyOnWriteArrayList<>();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        log.info("세션 [ {} ] 연결됨", session.getId());
//        sessionList.add(session);
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        String payload = message.getPayload();
//        log.info("메시지 받음: {}", payload);
//        broadcast(payload);
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        log.info("{} 연결 끊김", session.getId());
//        sessionList.remove(session);
//        broadcast(session.getId() + "님께서 퇴장하셨습니다.");
//    }
//
//    public void broadcast(String message) throws IOException {
//        for (WebSocketSession session : sessionList) {
//            session.sendMessage(new TextMessage(message));
//        }
//    }
//}
