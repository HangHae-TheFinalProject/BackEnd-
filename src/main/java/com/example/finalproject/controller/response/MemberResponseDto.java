package com.example.finalproject.controller.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MemberResponseDto {
    private Long memberId;
    private String email;
    private String nickname;
}
