package com.example.finalproject.controller;


import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.service.GameRoomService;
import com.example.finalproject.service.GameRoomServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
@ControllerAdvice
public class GemeRoomController {
    private final GameRoomService gameRoomService;
    private final GameRoomServiceImpl gameRoomServiceImpl;

    // 메인 페이지 (방 생성 및 방에 들어가기 위한 페이지)
    @GetMapping({"", "/", "/index", "/home", "/main"})
    public ModelAndView displayMainPage(
            final Long id,
            final String uuid) {
        log.info("메인페이지 이동 - id : {}, uuid(유저아이디 인듯) : {}", id, uuid);
        return this.gameRoomService.displayMainPage(id, uuid);
    }

    // 방 생성
    @PostMapping(value = "/room", params = "action=create")
    public ModelAndView processRoomSelection(
            @ModelAttribute("id") final String sid,
            @ModelAttribute("uuid") final String uuid,
            final BindingResult binding) {
        log.info("방 생성 - sid : {}, uuid(유저아이디 인듯) : {}", sid, uuid);
        return this.gameRoomService.processRoomSelection(sid, uuid, binding);
    }


    // 방 입장
    @GetMapping("/room/{sid}/user/{uuid}")
    public ModelAndView displaySelectedRoom(
            @PathVariable("sid") final String sid, // 방 id
            @PathVariable("uuid") final String uuid) { // 멤버 아이디
        log.info("방 입장 - sid : {}, uuid(유저아이디) : {}", sid, uuid);
        return this.gameRoomService.displaySelectedRoom(sid, uuid);
    }

    // 방 나가기
    @GetMapping("/room/{sid}/user/{uuid}/exit")
    public ModelAndView processRoomExit(
            @PathVariable("sid") final String sid,
            @PathVariable("uuid") final String uuid) {
        log.info("방 나가기 - sid : {}, uuid(유저아이디) : {}", sid, uuid);
        return this.gameRoomService.processRoomExit(sid, uuid);
    }

    // 방id 랜덤 생성
    @GetMapping("/room/random")
    public ModelAndView requestRandomRoomNumber(
            @ModelAttribute("uuid") final String uuid) {
        log.info("방id 랜덤 생성 - uuid(유저아이디) : {}", uuid);
        return gameRoomService.requestRandomRoomNumber(uuid);
    }

    // 샘플 오퍼
    @GetMapping("/offer")
    public ModelAndView displaySampleSdpOffer() {
        log.info("샘플 오퍼");
        return new ModelAndView("sdp_offer");
    }

    // 스트리밍
    @GetMapping("/stream")
    public ModelAndView displaySampleStreaming() {
        log.info("스트리밍");
        return new ModelAndView("streaming");
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 메인 페이지 OR 방 전체 목록 조회 (방 생성 및 방에 들어가기 위한 페이지) - json
    @GetMapping( "/rooms/test")
    public ResponseEntity<?> lierMainPage(HttpServletRequest request){
        return gameRoomServiceImpl.lierMainPage(request);
    }


    // 방 생성 - json
    @PostMapping( "/room/test")
    public ResponseEntity<?> makeGameRoom(
            @RequestBody GameRoomRequestDto gameRoomRequestDto,
            HttpServletRequest request){
        log.info("메인페이지 이동 - 방 이름 : {}, 방 패스워드 : {}, 게임 모드 : {}", gameRoomRequestDto.getRoomName(), gameRoomRequestDto.getRoomPassword(), gameRoomRequestDto.getMode());
        return gameRoomServiceImpl.makeGameRoom(gameRoomRequestDto, request);
    }


    // 방 입장 - json
    @GetMapping("/room/{roomId}/test")
    public ResponseEntity<?> enterGameRoom(
            @PathVariable Long roomId,
            HttpServletRequest request) { // 멤버 아이디
        log.info("방 입장 - 방 id : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomServiceImpl.enterGameRoom(roomId, request);
    }


    // 방 나가기 -jsno
    @GetMapping("/room/{roomId}/exit/test")
    public ResponseEntity<?> roomExit(
            @PathVariable Long roomId,
            HttpServletRequest request) {
        log.info("방 나가기 - 방 아이디 : {}, uuid(유저아이디) : {}", roomId, request);
        return gameRoomServiceImpl.roomExit(roomId, request);
    }

}
