package com.example.finalproject.controller;


import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.domain.GameMessage;
import com.example.finalproject.domain.GameRoom;
import com.example.finalproject.service.GameRoomService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

import static com.example.finalproject.domain.QGameRoom.gameRoom;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
@ControllerAdvice
public class GemeRoomController {

    private final GameRoomService gameRoomService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final JPAQueryFactory jpaQueryFactory;


    // 메인 페이지 OR 방 전체 목록 조회 (방 생성 및 방에 들어가기 위한 페이지) - json
    @GetMapping("/rooms")
    public ResponseEntity<?> lierMainPage(
            HttpServletRequest request) { // 인증정보를 가진 request
        return gameRoomService.lierMainPage(request);
    }

    // 방 생성 - json
    @PostMapping("/room")
    public ResponseEntity<?> makeGameRoom(
            @RequestBody GameRoomRequestDto gameRoomRequestDto, // 방 생성을 위한 정보를 기입할 DTO
            HttpServletRequest request) // 인증정보를 가진 request
            throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException {

        log.info("메인페이지 이동 - 방 이름 : {}, 방 패스워드 : {}, 게임 모드 : {}", gameRoomRequestDto.getRoomName(), gameRoomRequestDto.getRoomPassword(), gameRoomRequestDto.getMode());
        return gameRoomService.makeGameRoom(gameRoomRequestDto, request);
    }


    // 방 입장 - json
    @PostMapping("/room/{roomId}")
    public ResponseEntity<?> enterGameRoom(
            @PathVariable Long roomId, // 입장할 방 id
            HttpServletRequest request) { // 입장할 인증정보를 가진 request

        log.info("방 입장 - 방 id : {}, uuid(유저아이디) : {}", roomId, request);

        // /sub/topic/{roomid} 로 같은 공간에 존재하는 유저들에게 메세지 전달
        // /pub/chat/주소 -> 서버에 메세지 전달

        return gameRoomService.enterGameRoom(roomId, request);
    }

    // 방 입장 - json
    @PostMapping("/room/test/{roomId}")
    public ResponseEntity<?> enterGameRoomtest(
            @PathVariable Long roomId, HttpServletRequest request) { // 입장할 인증정보를 가진 request

        log.info("게임방 id : {}", roomId);

        return gameRoomService.enterGameRoom(roomId, request);

        // /sub/topic/{roomid} 로 같은 공간에 존재하는 유저들에게 메세지 전달
        // /pub/chat/주소 -> 서버에 메세지 전달
    }


    // 방 나가기 -jsno
    @DeleteMapping("/room/{roomId}/exit")
    public ResponseEntity<?> roomExit(
            @PathVariable Long roomId, // 나가고자 하는 방의 Id
            HttpServletRequest request) { // 나가고자하는 멤버의 인증정보를 가진 request
        log.info("방 나가기 - 방 아이디 : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomService.roomExit(roomId, request);
    }

    // openvidu 연결
//    @PostMapping("/openvidu")
//    public ResponseEntity<?> connectOpenvidu(
//            HttpServletRequest request
//    ) throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException{
//        return gameRoomServiceImpl.connectOpenvidu(request);
//    }


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    // stomp

    @MessageMapping("/lier/room/{roomId}") // /pub/ 이 맨앞에 붙어 발행 요청을 하게 될 것
    @SendTo("/sub/lier/room/test/{roomId}")
    public void message(@DestinationVariable String roomId, GameMessage gameMessage) {
        if (GameMessage.MessageType.JOIN.equals(gameMessage.getType())) {
            gameMessage.setRoomId(roomId);
            gameMessage.setContent(gameMessage.getRoomId() + "방에 " + gameMessage.getSender() + "님이 입장하셨습니다.");
        }
        System.out.println("내용 : " + gameMessage.getContent());

        messagingTemplate.convertAndSend("/sub/lier/room/test/" + Long.parseLong(gameMessage.getRoomId()), gameMessage);
    }


    // 프론트 쪽에서 /ws-stomp 경로의 stomp 서버 연결을 해주고
    // 프론트 쪽에서 이 api url을 주소를 subscribe(구독) 해주어야 작동함 (맨앞에 /sub 경로가 붙어있음)
    @PostMapping("/gameroom/{roomId}")
    public void roomMessageProxy(@Payload GameMessage message) throws JsonProcessingException {

        System.out.println("gameMessage 진입 확인");

        if (GameMessage.MessageType.JOIN.equals(message.getType())) {
            System.out.println("여기에 들어오나" + message.getType());
            join(message);
        }

        if (GameMessage.MessageType.UPDATE.equals(message.getType())) {
            System.out.println("여기에 들어오나" + message.getType());
            update(message);
        }
//        if (GameMessage.MessageType.READY.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            ready(message);
//        }
//
//        if (GameMessage.MessageType.SWITCHING.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            switchingPosition(message);
//        }
    }

    // 게임 방 진입 시 받은 메세지로 후처리
    private void join(GameMessage message) throws JsonProcessingException {
        String roomId = message.getRoomId();
        GameRoom room = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(Long.parseLong(roomId)))
                .fetchOne();

        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(roomId);
        gameMessage.setSender(message.getSender());
        gameMessage.setContent("게임 방 진입");
        gameMessage.setType(GameMessage.MessageType.UPDATE);
        messagingTemplate.convertAndSend("/sub/groom/" + roomId, gameMessage);
    }


    private void update(GameMessage message) throws JsonProcessingException {
        String roomId = message.getRoomId();
//        GameRoom room = gameRoomRepository.findByRoomId(roomId);
        GameRoom existGameRoom = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(Long.parseLong(roomId)))
                .fetchOne();

        if (existGameRoom != null) {
//            String userListMessage = jsonStringBuilder.gameRoomResponseDtoJsonBuilder(room);
            GameMessage gameMessage = new GameMessage();
            gameMessage.setRoomId(roomId);
            gameMessage.setSender(message.getSender());
//            gameMessage.setContent(userListMessage);
            gameMessage.setType(GameMessage.MessageType.UPDATE);
            messagingTemplate.convertAndSend("/sub/groom/" + roomId, gameMessage);
        }
    }

}
