package com.example.finalproject.controller.response;

import com.example.finalproject.domain.Media;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PostResponseDto {

    private Long postId;
    private String author;
    private String title;
    private String content;
    private List<Media> medias;
}
