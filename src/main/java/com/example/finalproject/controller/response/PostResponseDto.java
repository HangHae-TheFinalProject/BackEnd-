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
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private List<Media> medias;
    private List<CommentResponseDto> comments;
}
