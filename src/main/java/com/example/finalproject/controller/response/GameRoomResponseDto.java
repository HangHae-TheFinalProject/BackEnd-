package com.example.finalproject.controller.response;

import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Mode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Builder
@Getter
public class GameRoomResponseDto {
    private Long id;
    private String roomName;
    private String roomPassword;
    private Mode mode;
    private List<Member> member;
    private String owner;
}
