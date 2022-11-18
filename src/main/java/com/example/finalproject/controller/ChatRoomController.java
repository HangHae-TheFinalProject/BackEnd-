package com.example.finalproject.controller;

import com.example.finalproject.domain.ChatRoom;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.repository.ChatRoomRepository;
import com.example.finalproject.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// WebSocket 통신 외에 채팅 화면 View 구성을 위해 필요한 Controller를 생성
@RequiredArgsConstructor
@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    private final ChatRoomService chatRoomService;

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    @ResponseBody
    public List<ChatRoom> room() {
        return chatRoomRepository.findAllRoom();
    }

    // 채팅방 생성
    @PostMapping("/room")
    @ResponseBody
    public ResponseEntity<PrivateResponseBody> createRoom(@RequestParam("name") String name, HttpServletRequest request) {
        return chatRoomService.createChatRoom(name, request);
    }

    // 채팅방 입장 화면
    @GetMapping(value = "/room/enter/{roomId}")
    public ResponseEntity<PrivateResponseBody> roomDetail(@PathVariable String roomId, HttpServletRequest request) {
        return chatRoomService.enterRoom(roomId, request); //닉네임, roomId 반환
    }

}