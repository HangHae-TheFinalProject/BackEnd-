package com.example.finalproject.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class GameRoomRequestDto {
    private String roomName;
    private String roomPassword = "";
    private Integer mode;

}
