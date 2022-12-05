package com.example.finalproject.shared;

import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final TokenProvider tokenProvider;

    /*
    websocket을 통해 들어온 요청이 처리 되기전 실행된다.
    필요한 경우 메시지를 수정할 수 있음
    이 메서드가 null을 반환하면 호출을 실제로 보내지 않음
    */

    // stomp 연결하기 전에 사용이 가능한 정상적인 토큰인지 유효성 검증을 하는 메소드
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        //메시지의 페이로드 및 헤더에서 인스턴스를 만듬
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        System.out.println("accessor = " + accessor);

        // 연결할때
        if (StompCommand.CONNECT == accessor.getCommand()) {

            String accessToken = Objects.requireNonNull(accessor.getFirstNativeHeader("Authorization")).substring(7);
            String refreshToken = accessor.getFirstNativeHeader("Refresh-Token");

            System.out.println("refreshToken = " + refreshToken);
            System.out.println("accessToken = " + accessToken);

            tokenProvider.validateToken(accessToken);
            tokenProvider.validateToken(refreshToken);

            log.info("Authorization validity is {}",tokenProvider.validateToken(accessToken));
            log.info("RefreshToken validity is {}",tokenProvider.validateToken(refreshToken));
        }

        return message;
    }
}
