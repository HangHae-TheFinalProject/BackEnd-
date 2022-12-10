package com.example.finalproject.controller;

import com.example.finalproject.service.GameRoomService;
import com.example.finalproject.service.TestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/test")
@RestController
@ControllerAdvice
public class Test {
    private final TestService gameRoomService;
    @PostMapping("/room/{roomId}")
    public ResponseEntity<?> enterGameRoom(
            @PathVariable Long roomId, // 입장할 방 id
            HttpServletRequest request) { // 입장할 인증정보를 가진 request
        log.info("방 입장 - 방 id : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomService.enterGameRoom(roomId, request);
    }

    @DeleteMapping("/room/{roomId}/exit")
    public ResponseEntity<?> roomExit(
            @PathVariable Long roomId, // 나가고자 하는 방의 Id
            HttpServletRequest request) { // 나가고자하는 멤버의 인증정보를 가진 request
        log.info("방 나가기 - 방 아이디 : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomService.roomExit(roomId, request);
    }

}