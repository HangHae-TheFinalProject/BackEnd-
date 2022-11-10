package com.example.finalproject.controller;

import com.example.finalproject.controller.response.ResponseDto;
import com.example.finalproject.domain.ChatRoom;
import com.example.finalproject.domain.Info;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.ChatRoomRepository;
import com.example.finalproject.service.ChatRoomService;
import com.example.finalproject.service.testService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// WebSocket 통신 외에 채팅 화면 View 구성을 위해 필요한 Controller를 생성
@RequiredArgsConstructor
@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;
    private final TokenProvider jwtTokenProvider;
    private final testService testService;
    private final ChatRoomService chatRoomService;


    // 채팅 리스트 화면
    @GetMapping("/example/room")
    public String rooms(Model model) {
        return "/chat/room";
    }

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    @ResponseBody
    public List<ChatRoom> room() {
        return chatRoomRepository.findAllRoom();
    }

    // 채팅방 생성
    @PostMapping("/room")
    @ResponseBody
    public ChatRoom createRoom(@RequestParam String name, HttpServletRequest request) {
        return chatRoomService.createChatRoom(name, request);
    }

    // 채팅방 입장 화면
    @GetMapping(value="/room/enter/{roomId}")
    public ResponseEntity<PrivateResponseBody> roomDetail(@PathVariable String roomId, HttpServletRequest request) {
        return chatRoomService.enterRoom(roomId, request); //닉네임, roomId 반환
    }

//    @GetMapping("/example/room/enter/{roomId}")
//    public String roomDetailExample(Model model, @PathVariable String roomId) {
//        model.addAttribute("roomId", roomId);
//        return "/chat/roomdetail";
//    }
//
//    // 특정 채팅방 조회
//    @GetMapping("/room/{roomId}")
//    @ResponseBody
//    public ChatRoom roomInfo(@PathVariable String roomId) {
//        return chatRoomRepository.findRoomById(roomId);
//    }
//
//    @GetMapping(value = "/test")
//    public ResponseEntity<PrivateResponseBody> testrequest(HttpServletRequest request) {
//        System.out.println("here");
//        return testService.testrequest(request);
//    }

//    @GetMapping("/user")
//    @ResponseBody
//    public LoginInfo getUserInfo() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String name = auth.getName();
//        return LoginInfo.builder().name(name).token(jwtTokenProvider.generateToken(name)).build();
//    }
}