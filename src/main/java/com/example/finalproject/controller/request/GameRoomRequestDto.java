package com.example.finalproject.controller.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameRoomRequestDto {
    private String roomName;
    private String roomPassword = null;
    private String mode;
    private String owner;


}
