package com.example.finalproject.controller;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RequestMapping("/lier/search")
@RestController
public class SearchController {

    private final SearchService searchService;

    // 게시글 검색
    @PostMapping("/post")
    public ResponseEntity<PrivateResponseBody> searchPost(
            HttpServletRequest request, // 인증 정보
            @RequestBody StringDto stringDto){ // 검색 키워드

        return searchService.searchPost(request, stringDto);
    }

    // 게시글 검색
    @PostMapping("/room")
    public ResponseEntity<PrivateResponseBody> searchRoom(
            HttpServletRequest request, // 인증 정보
            @RequestBody StringDto stringDto){ // 검색 키워드

        return searchService.searchRoom(request, stringDto);
    }
}
