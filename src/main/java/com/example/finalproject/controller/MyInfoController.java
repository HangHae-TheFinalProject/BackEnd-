package com.example.finalproject.controller;

import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.service.MyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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


    // 회웑정보 + 전적 조회
    @GetMapping("/allrecord")
    public ResponseEntity<PrivateResponseBody>  getMyAllRecord(
            HttpServletRequest request){
        return myInfoService.getMyAllRecord(request);
    }

    // 리워드 조회
    @GetMapping("/reward/{pageNum}")
    public ResponseEntity<PrivateResponseBody> getMyReward(
            HttpServletRequest request,
            @PathVariable Integer pageNum){
        return myInfoService.getMyReward(request, pageNum);
    }


}
