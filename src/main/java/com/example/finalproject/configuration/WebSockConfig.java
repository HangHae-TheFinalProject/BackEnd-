package com.example.finalproject.configuration;

import com.example.finalproject.shared.CustomHandshakeHandler;
import com.example.finalproject.shared.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSocketMessageBroker
public class WebSockConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub"); // 구독한 것에 대한 경로. 이쪽으로 메세지를 보내면 전체적으로 브로드캐스팅
        config.setApplicationDestinationPrefixes("/pub"); //
    }

    // 1. 프론트에서 연결되는 작업을 해줘야함 /ws-stomp 경로를 실행해야함
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS();
    }
    // 소켓 연결 하기 전 유효성 검사하기 위해 만들어 놓은 stompHandler 등록
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }
}