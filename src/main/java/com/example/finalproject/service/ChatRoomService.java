package com.example.finalproject.service;

import com.example.finalproject.domain.ChatRoom;
import com.example.finalproject.controller.response.ChatRoomDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.ChatRoomRepository;
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

    public ResponseEntity<PrivateResponseBody> createChatRoom(String roomId, String name) {
//        Member member = validateMember(request);
//        if (null == member) {
//            return new ResponseEntity<>(new PrivateResponseBody
//                    (StatusCode.LOGIN_MEMBER_ID_FAIL, null), HttpStatus.BAD_REQUEST);
//        }
        ChatRoom chatRoom = ChatRoom.create(name, roomId);
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, chatRoomRepository.saveRoom(chatRoom)), HttpStatus.OK);
    }
    public ResponseEntity<PrivateResponseBody> enterRoom(String roomId, HttpServletRequest request) {
        Member member = validateMember(request);
        if (null == member) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_MEMBER_ID_FAIL, null), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, ChatRoomDto.builder()
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
