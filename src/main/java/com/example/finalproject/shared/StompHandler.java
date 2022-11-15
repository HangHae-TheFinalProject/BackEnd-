package com.example.finalproject.shared;

import com.example.finalproject.domain.ChatMessage;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.UserDetailsImpl;
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
import java.util.Optional;


//public class StompHandler implements ChannelInterceptor {
//
//    private final TokenProvider jwtTokenProvider;
//
//    // websocket을 통해 들어온 요청이 처리 되기전 실행된다.
//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
//        // websocket 연결시 헤더의 jwt token 검증
//        if (StompCommand.CONNECT == accessor.getCommand()) {
//            jwtTokenProvider.validateToken(accessor.getFirstNativeHeader("Refresh-Token"));
//        }
//        return message;
//    }
//}

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final TokenProvider tokenProvider;
    private final ChatRoomRepository chatRoomRepository;

    /*
    websocket을 통해 들어온 요청이 처리 되기전 실행된다.
    필요한 경우 메시지를 수정할 수 있음
    이 메서드가 null을 반환하면 호출을 실제로 보내지 않음
    */

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        //메시지의 페이로드 및 헤더에서 인스턴스를 만듬
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        System.out.println("accessor = " + accessor);

        // 연결할때
        if (StompCommand.CONNECT == accessor.getCommand()) {
            //intial connection => Token 유효성 검사
            //Access Token invalid => reissue
            //Refresh Token invalid => login again

            String accessToken = Objects.requireNonNull(accessor.getFirstNativeHeader("Authorization")).substring(7);
            String refreshToken = accessor.getFirstNativeHeader("Refresh-Token");
            System.out.println("refreshToken = " + refreshToken);
            System.out.println("accessToken = " + accessToken);
            tokenProvider.validateToken(accessToken);
            tokenProvider.validateToken(refreshToken);
            log.info("Authorization validity is {}",tokenProvider.validateToken(accessToken));
            log.info("RefreshToken validity is {}",tokenProvider.validateToken(refreshToken));
//            accessor.setUser(new User(accessor.getLogin());
        }

        // 방 구독할 때
//        else if (StompCommand.SUBSCRIBE == accessor.getCommand()){
//
//            //header destination = /sub/chat/room/{roomId}
//            String destination = Optional.ofNullable((String)message.getHeaders().get("simpDestination"))
//                    .orElse("Invalid RoomId");
//            log.info("message destination={}", destination);
//            log.info("message header info {}", message.getHeaders());
//
//            //roomId get
//            String roomId = chatRoomService.getRoomId(destination);
//            System.out.println("roomId 111111111111111111111111111111111111111111= " + roomId);
//            //Client마다 SessionID 생성 => ChatRoomId와 mapping
//            String sessionId = (String) message.getHeaders().get("simpSessionId");
//            log.info("session Id is {}", sessionId);
//            chatRoomRepository.setUserEnterInfo(sessionId, roomId);
//
//            //채팅방 인원수 +1
//            chatRoomRepository.plusUserCount(roomId);
////            String name = Optional.ofNullable((Principal) message.getHeaders()
////                    .get("simpUser")).map(Principal::getName).orElse("UnknownUser");
//            Authentication authentication = tokenProvider.getAuthentication(Objects.requireNonNull(accessor.getFirstNativeHeader("Authorization")));
//
//            log.info("authentication is {}",authentication);
//            Member member = ((UserDetailsImpl) authentication.getPrincipal()).getMember();
//            //이름 그냥 넣어주기 로그인 정보에서
////            Member member = tokenProvider.getMemberFromAuthentication();
//            log.info("member INFO {}",member);
//            System.out.println("message12312412412421412412421 = " + message);
//            System.out.println(" simpleUser= " +message.getHeaders()
//                    .get("simpUser"));
////            System.out.println("name111111111111111111111111111111111111 = " + member.getNickName());
//
//            chatMessageService.sendChatMessage(ChatMessage.builder()
//                    .type(ChatMessage.MessageType.ENTER)
//                    .roomId(roomId)
//                    .sender(member.getNickName())
//                    .build());
//
//            log.info("SUBSCRIBED {}, {}", member.getNickName(), roomId);
//        }
//
//        else if (StompCommand.DISCONNECT == accessor.getCommand()){
//            // 연결이 종료된 클라이언트 sesssionId로 채팅방 id를 얻는다.
//            String sessionId = (String) message.getHeaders().get("simpSessionId");
//            String roomId = chatRoomRepository.getUserEnterRoomId(sessionId);
//
//            Authentication authentication = tokenProvider.getAuthentication(Objects.requireNonNull(accessor.getFirstNativeHeader("Authorization")));
//
//            log.info("authentication is {}",authentication);
//            Member member = ((UserDetailsImpl) authentication.getPrincipal()).getMember();
//            // 채팅방의 인원수를 -1한다.
//            chatRoomRepository.minusUserCount(roomId);
//            // 클라이언트 퇴장 메시지를 채팅방에 발송한다.(redis publish)
////            String name = Optional.ofNullable((Principal) message.getHeaders().get("simpUser")).map(Principal::getName).orElse("UnknownUser");
//            chatMessageService.sendChatMessage(ChatMessage.builder()
//                    .type(ChatMessage.MessageType.QUIT)
//                    .roomId(roomId)
//                    .sender(member.getNickName())
//                    .build());
//            // 퇴장한 클라이언트의 roomId 맵핑 정보를 삭제한다.
//            chatRoomRepository.removeUserEnterInfo(sessionId);
//            log.info("DISCONNECTED {}, {}", sessionId, roomId);
//        }


        //return ChannelInterceptor.super.preSend(message, channel);
        return message;
    }
}
