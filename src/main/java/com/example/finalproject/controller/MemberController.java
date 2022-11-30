package com.example.finalproject.controller;

import com.example.finalproject.configuration.SwaggerAnnotation;
import com.example.finalproject.controller.request.LoginRequestDto;
import com.example.finalproject.controller.request.MemberRequestDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Slf4j
@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<PrivateResponseBody> signup(
            @RequestBody MemberRequestDto memberRequestDto){
        // 회원가입 진행시 재확인용 비밀번호는 입력하지 않는지 확인

        log.info("이메일 : {}, 닉네임 : {}, 패스워드 : {}", memberRequestDto.getEmail(), memberRequestDto.getNickname(), memberRequestDto.getPassword());
        return memberService.signup(memberRequestDto);
    }

    // 로그인
    @PostMapping(value = "/login")
    public ResponseEntity<PrivateResponseBody> login(
            @RequestBody LoginRequestDto requestDto,
            HttpServletResponse response) {
        log.info("아이디 : {}, 비밀번호 : {}, response 헤더 : {}", requestDto.getEmail(), requestDto.getPassword(), response.getHeader("Authorization"));
        return memberService.login(requestDto, response);
    }

    // 로그아웃
    @SwaggerAnnotation
    @PostMapping(value = "/logout")
    public ResponseEntity<PrivateResponseBody> logout(
            HttpServletRequest request) {
        return memberService.logout(request);
    }

    // 회원탈퇴
    @DeleteMapping(value = "/removal")
    public ResponseEntity<PrivateResponseBody> cleansing(
            HttpServletRequest request
    ) {
        return memberService.cleansing(request);
    }

}
