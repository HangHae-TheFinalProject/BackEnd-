package com.example.finalproject.controller.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaResponseDto {

    private Long mediaId;
    private String mediaName;
    private String mediaUrl;

}
