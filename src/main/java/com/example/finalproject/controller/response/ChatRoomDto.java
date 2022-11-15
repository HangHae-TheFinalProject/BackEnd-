package com.example.finalproject.controller.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ChatRoomDto {
    private String nickname;
    private String roomId;

}
