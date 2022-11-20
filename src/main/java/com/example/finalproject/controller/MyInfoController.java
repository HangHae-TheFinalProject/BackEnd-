package com.example.finalproject.controller;

import com.example.finalproject.service.MyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RequestMapping("/lier/myinfo")
@RestController
public class MyInfoController {

    private final MyInfoService myInfoService;

    // 작성한 게시글 조회
    @GetMapping("/posts")
    public ResponseEntity<?> getMyPosts(
            HttpServletRequest request){

        return myInfoService.getMyPosts(request);
    }

    // 작성한 댓글 조회
//    @GetMapping("/comments")
//    public ResponseEntity<?> getMyComments(
//            HttpServletRequest request){
//
//        return myInfoService.getMyComments(request);
//    }



}
