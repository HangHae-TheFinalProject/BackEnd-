package com.example.finalproject.controller;


import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
@ControllerAdvice
public class GemeRoomController {

    private final GameRoomService gameRoomService;

    // 메인 페이지 OR 방 전체 목록 조회 (방 생성 및 방에 들어가기 위한 페이지) - 페이징 처리 완료
    @GetMapping("/rooms/{pageNum}/view/{view}")
    public ResponseEntity<?> lierMainPage(
            HttpServletRequest request,
            @PathVariable int pageNum,
            @PathVariable String view){ // 인증정보를 가진 request
        return gameRoomService.lierMainPage(request, pageNum, view);
    }


    // 방 생성 - json
    @PostMapping("/room")
    public ResponseEntity<?> makeGameRoom(
            @RequestBody GameRoomRequestDto gameRoomRequestDto, // 방 생성을 위한 정보를 기입할 DTO
            HttpServletRequest request) // 인증정보를 가진 request
            throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException
    {
        log.info("메인페이지 이동 - 방 이름 : {}, 방 패스워드 : {}, 게임 모드 : {}", gameRoomRequestDto.getRoomName(), gameRoomRequestDto.getRoomPassword(), gameRoomRequestDto.getMode());
        return gameRoomService.makeGameRoom(gameRoomRequestDto, request);
    }


    // 방 입장 - json
    @PostMapping("/room/{roomId}")
    public ResponseEntity<?> enterGameRoom(
            @PathVariable Long roomId, // 입장할 방 id
            HttpServletRequest request,
            Principal principal) { // 입장할 인증정보를 가진 request
        log.info("방 입장 - 방 id : {}, uuid(유저아이디) : {}, 프린시팔 : {}", roomId, request, principal);
        return gameRoomService.enterGameRoom(roomId, request, principal);
    }


    // 방 나가기 -jsno
    @DeleteMapping("/room/{roomId}/exit")
    public ResponseEntity<?> roomExit(
            @PathVariable Long roomId, // 나가고자 하는 방의 Id
            HttpServletRequest request) { // 나가고자하는 멤버의 인증정보를 가진 request
        log.info("방 나가기 - 방 아이디 : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomService.roomExit(roomId, request);
    }


}
