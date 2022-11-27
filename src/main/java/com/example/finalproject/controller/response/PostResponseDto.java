package com.example.finalproject.controller.response;

import com.example.finalproject.domain.Comment;
import com.example.finalproject.domain.Media;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostResponseDto {

    private Long postId;
    private String author;
    private String title;
    private String content;
    private Long viewcnt;
    private String createdAt;
    private String modifiedAt;
    private List<MediaResponseDto> medias;
    private List<CommentResponseDto> comments;
}
