package com.example.finalproject.service;

import com.example.finalproject.controller.response.ResponseDto;
import com.example.finalproject.domain.ChatRoom;
import com.example.finalproject.domain.Info;
import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.ChatRoomRepository;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@Service
public class ChatRoomService {
    private final TokenProvider jwtTokenProvider;
    private final ChatRoomRepository chatRoomRepository;

    public ChatRoom createChatRoom(String name, HttpServletRequest request) {
        Member member = validateMember(request);
        if (null == member) {
            System.out.println("멤버 없음");
            return null;
        }
        ChatRoom chatRoom = ChatRoom.create(name);
        return chatRoomRepository.saveRoom(chatRoom);

    }
    public ResponseEntity<PrivateResponseBody> enterRoom(String roomId, HttpServletRequest request) {
        Member member = validateMember(request);
        if (null == member) {
            System.out.println("멤버 없음");
            return null;
        }
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, Info.builder()
                .roomId(roomId)
                .nickname(member.getNickname())
                .build()), HttpStatus.OK);
    }

    @Transactional
    public Member validateMember(HttpServletRequest request) {
        if (!jwtTokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return null;
        }
        return jwtTokenProvider.getMemberFromAuthentication();
    }
}
