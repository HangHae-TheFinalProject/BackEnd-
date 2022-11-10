package com.example.finalproject.controller;

import com.example.finalproject.domain.ChatMessage;
import com.example.finalproject.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatController {

    private final ChatService ChatService;

    /**
     * websocket "/pub/chat/message"로 들어오는 메시징을 처리한다.
     */
//    @MessageMapping("/chat/message")
//    public void message(ChatMessage message, HttpServletRequest request) {
//        ChatService.meesage(message, request);
//
//    }
    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        ChatService.meesage(message);

    }

}
