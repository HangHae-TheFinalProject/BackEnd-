package com.example.finalproject.shared;

import org.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSockChatHandler extends TextWebSocketHandler {
//    private static List<WebSocketSession> list = new ArrayList<>();
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        String payload = message.getPayload();
//        JSONObject jsonObject = new JSONObject(payload);
//
//        for(WebSocketSession sess: list) {
////            sess.sendMessage(message);
//            sess.sendMessage(new TextMessage("Hi " + jsonObject.getString("user") + "!"));
//
//        }
//    }
//
//    /* Client가 접속 시 호출되는 메서드 */
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//
//        list.add(session);
//
////        log.info(session + " 클라이언트 접속");
//    }
//
//    /* Client가 접속 해제 시 호출되는 메서드드 */
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//
////        log.info(session + " 클라이언트 접속 해제");
//        list.remove(session);
//    }

    //웹 소켓 연결이 수립된 후, 해당 연결을 이용하는 세션과 메시지에 대해 다룬다.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println(message);
        System.out.println(message.getPayload());
    }

    //연결이 수립된 직후의 행위를 지정
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("afterConnectionEstablished:" + session.toString());
    }

    //연결이 닫힌 직후의 행위를 지정
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

}