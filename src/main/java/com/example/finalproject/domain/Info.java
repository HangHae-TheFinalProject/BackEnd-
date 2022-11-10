package com.example.finalproject.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Info {
    private String nickname;
    private String roomId;

//    public Info(String nickname, String roomId) {
//        this.nickname = nickname;
//        this.roomId = roomId;
//    }
}
