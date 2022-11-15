package com.example.finalproject.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
@AllArgsConstructor
@Getter
@Builder
public class PostRequestDto {

    private String title;
    private String content;

}
