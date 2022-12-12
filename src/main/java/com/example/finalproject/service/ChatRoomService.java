package com.example.finalproject.service;

import com.example.finalproject.domain.ChatRoom;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public ResponseEntity<PrivateResponseBody> createChatRoom(String roomId, String name) {

        ChatRoom chatRoom = ChatRoom.create(name, roomId);
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, chatRoomRepository.saveRoom(chatRoom)), HttpStatus.OK);
    }

}
