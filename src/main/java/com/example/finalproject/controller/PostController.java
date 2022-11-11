package com.example.finalproject.controller;

import com.example.finalproject.controller.request.PostRequestDto;
import com.example.finalproject.service.PostService;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
public class PostController {

    private final PostService postService;

    // 게시글 작성
    @PostMapping(value="/post", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity<?> writePost(
            HttpServletRequest request,
            @RequestBody PostRequestDto postRequestDto,
            @RequestParam(value="media", required = false) List<MultipartFile> multipartFiles){

        return postService.writePost(request, postRequestDto, multipartFiles);
    }

    // 게시글 수정
    @PutMapping(value = "/post/{postId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity<?> updatePost(
            HttpServletRequest request,
            @PathVariable Long postId,
            @RequestParam(value="media", required = false) List<MultipartFile> multipartFiles,
            @RequestBody PostRequestDto postRequestDto){

        return postService.updatePost(request, postId, multipartFiles,postRequestDto);
    }

    // 게시글 삭제
    @DeleteMapping("/post/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            HttpServletRequest request){
        return postService.deletePost(postId, request);
    }


    // 게시글 상세 조회
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getPost(
            HttpServletRequest request,
            @PathVariable Long postId){
        return postService.getPost(request, postId);
    }

    // 게시글 전체 목록 조회
    @GetMapping("/posts")
    public ResponseEntity<?> getAllPost(
            HttpServletRequest request,
            @PageableDefault(page =0, size = 10 ,sort ="createdAt",direction = Sort.Direction.DESC) Pageable pageable){
        return postService.getAllPost(request, pageable);
    }



}
