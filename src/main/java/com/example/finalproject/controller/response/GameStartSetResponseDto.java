package com.example.finalproject.controller.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameStartSetResponseDto {
    private String lier;
    private String category;
    private String keyword;
    private Long roomId;
}
