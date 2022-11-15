package com.example.finalproject.controller;


import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
@ControllerAdvice
public class GemeRoomController {

    private final GameRoomService gameRoomService;

    // 메인 페이지 OR 방 전체 목록 조회 (방 생성 및 방에 들어가기 위한 페이지) - json
    @GetMapping( "/rooms")
    public ResponseEntity<?> lierMainPage(HttpServletRequest request){
        return gameRoomService.lierMainPage(request);
    }


    // 방 생성 - json
    @PostMapping( "/room")
    public ResponseEntity<?> makeGameRoom(
            @RequestBody GameRoomRequestDto gameRoomRequestDto,
            HttpServletRequest request) throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException{
        log.info("메인페이지 이동 - 방 이름 : {}, 방 패스워드 : {}, 게임 모드 : {}", gameRoomRequestDto.getRoomName(), gameRoomRequestDto.getRoomPassword(), gameRoomRequestDto.getMode());
        return gameRoomService.makeGameRoom(gameRoomRequestDto, request);
    }


    // 방 입장 - json
    @PostMapping("/room/{roomId}")
    public ResponseEntity<?> enterGameRoom(
            @PathVariable Long roomId,
            HttpServletRequest request) { // 멤버 아이디
        log.info("방 입장 - 방 id : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomService.enterGameRoom(roomId, request);
    }


    // 방 나가기 -jsno
    @DeleteMapping("/room/{roomId}/exit")
    public ResponseEntity<?> roomExit(
            @PathVariable Long roomId,
            HttpServletRequest request) {
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



}
