package com.example.finalproject.service;

import com.example.finalproject.domain.ChatMessage;
import com.example.finalproject.domain.Member;

import com.example.finalproject.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@Service
public class ChatService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;
    private final TokenProvider jwtTokenProvider;
//    public void meesage(ChatMessage message, HttpServletRequest request) {
//        Member member = validateMember(request);
//        if (null == member) {
//            System.out.println("멤버 없음");
//            return;
//        }
//        String nickname = member.getNickname();
//        // 로그인 회원 정보로 대화명 설정
//        message.setSender(nickname);
//        message.setRoomId("97a853e7-73d1-41a2-a380-71b28e07b02e");
//        // 채팅방 입장시에는 대화명과 메시지를 자동으로 세팅한다.
//        if (ChatMessage.MessageType.ENTER.equals(message.getType())) {
//            message.setSender("[알림]");
//            message.setMessage(nickname + "님이 입장하셨습니다.");
//        }
//        // Websocket에 발행된 메시지를 redis로 발행(publish)
//        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
//    }
public void meesage(ChatMessage message) {

    String nickname = "test";
    // 로그인 회원 정보로 대화명 설정
    message.setSender(nickname);
//    message.setRoomId("97a853e7-73d1-41a2-a380-71b28e07b02e");
    // 채팅방 입장시에는 대화명과 메시지를 자동으로 세팅한다.
    if (ChatMessage.MessageType.ENTER.equals(message.getType())) {
        message.setSender("[알림]");
        message.setMessage(nickname + "님이 입장하셨습니다.");
    }
    // Websocket에 발행된 메시지를 redis로 발행(publish)
    redisTemplate.convertAndSend(channelTopic.getTopic(), message);
}

    @Transactional
    public Member validateMember(HttpServletRequest request) {
        if (!jwtTokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return null;
        }
        return jwtTokenProvider.getMemberFromAuthentication();
    }
}
