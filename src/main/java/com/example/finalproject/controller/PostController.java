package com.example.finalproject.controller;

import com.example.finalproject.controller.request.PostRequestDto;
import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
public class PostController {

    private final PostService postService;

    // 게시글 작성
    @PostMapping(value="/post", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity<PrivateResponseBody> writePost(
            HttpServletRequest request, // 인증 정보를 가진 request
            @RequestBody PostRequestDto postRequestDto, // 게시글 작성 내용이 담긴 Dto
            @RequestParam(value="media", required = false) List<MultipartFile> multipartFiles){ // 혹시 모를 이미지 파일을 업로드 하기 위한 multipartfile

        return postService.writePost(request, postRequestDto, multipartFiles);
    }

    // 게시글 수정
    @PutMapping(value = "/post/{postId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity<PrivateResponseBody> updatePost(
            HttpServletRequest request, // 인증 정보를 가진 request
            @PathVariable Long postId, // 수정할 게시글 id
            @RequestParam(value="media", required = false) List<MultipartFile> multipartFiles, // 혹시 모를 이미지 파일을 수정 업로드 하기 위한 multipartfile
            @RequestBody PostRequestDto postRequestDto){ // 게시글 수정 정보가 담긴 Dto

        return postService.updatePost(request, postId, multipartFiles,postRequestDto);
    }

    // 게시글 삭제
    @DeleteMapping("/post/{postId}")
    public ResponseEntity<PrivateResponseBody> deletePost(
            @PathVariable Long postId, // 삭제할 게시글 id
            HttpServletRequest request){ // 인증 정보를 가진 request

        return postService.deletePost(postId, request);
    }


    // 게시글 상세 조회
    @GetMapping("/post/{postId}")
    public ResponseEntity<PrivateResponseBody> getPost(
            HttpServletRequest request, // 인증 정보를 가진 request
            @PathVariable Long postId){ // 상세 조회할 게시글 id

        return postService.getPost(request, postId);
    }


    // 게시글 전체 목록 조회
//    @GetMapping("/posts/{pageNum}")
    @GetMapping("/posts/{sort}")
    public ResponseEntity<PrivateResponseBody> getAllPost(
            HttpServletRequest request, // 인증 정보를 가진 request
            @PathVariable String sort
//            @PathVariable Integer pageNum // 페이지 번호
    ){
        log.info("전체 목록 조회 : {}", sort);
        return postService.getAllPost(request,sort);
    }



}
