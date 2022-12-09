package com.example.finalproject.service;

import com.example.finalproject.domain.ChatMessage;
import com.example.finalproject.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChatService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

public void meesage(ChatMessage message) {

    // Websocket에 발행된 메시지를 redis로 발행(publish)
    redisTemplate.convertAndSend(channelTopic.getTopic(), message);
}

}
