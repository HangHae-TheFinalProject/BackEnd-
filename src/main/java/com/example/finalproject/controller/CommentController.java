package com.example.finalproject.controller;

import com.example.finalproject.controller.request.CommentRequestDto;
import com.example.finalproject.controller.response.ResponseDto;
import com.example.finalproject.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/lier")
public class CommentController {
    private final CommentService commentService;

    // 작성
    @PostMapping(value = "/comment/{postid}")
    public ResponseEntity<?> writeComment(@PathVariable Long postid, @RequestBody CommentRequestDto requestDto,
                                          HttpServletRequest request) {
        return commentService.writeComment(postid, requestDto, request);
    }
    // 수정
    @PutMapping(value = "/comment/{commentid}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentid, @RequestBody CommentRequestDto requestDto,
                                          HttpServletRequest request) {
        return commentService.updateComment(commentid, requestDto, request);
    }
    // 삭제
    @DeleteMapping("/comment/{commentid}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentid, HttpServletRequest request){
        return commentService.deleteComment(commentid, request);
    }
}
