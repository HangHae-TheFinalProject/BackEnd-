package com.example.finalproject.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
  private Long commentid;
  private String content;
  private String author;
  private String createdAt;
  private String modifiedAt;
}
