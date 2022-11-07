package com.example.finalproject.configuration;

import com.example.finalproject.shared.WebSockChatHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSocket
public class WebSockConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/myHandler")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler myHandler() {
        return new WebSockChatHandler();
    }
}

//public class WebSockConfig implements WebSocketMessageBrokerConfigurer {
//    //    private final WebSocketHandler webSocketHandler;
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/webSocket")
//                .setAllowedOrigins("http://localhost:8080")
//                .withSockJS();
//    }
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        registry.enableSimpleBroker("/topic","/queue");
//        registry.setApplicationDestinationPrefixes("/");
//    }
//}