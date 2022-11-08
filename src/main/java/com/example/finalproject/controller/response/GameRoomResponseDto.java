package com.example.finalproject.controller.response;

import com.example.finalproject.domain.Member;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Builder
@Getter
public class GameRoomResponseDto {
    private Long id;
    private String roomName;
    private String mode;
    private List<Member> member;
    private String owner;
//    private Integer playerCnt = 0;
}
