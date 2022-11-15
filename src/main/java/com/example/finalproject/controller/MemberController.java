package com.example.finalproject.controller;

import com.example.finalproject.configuration.SwaggerAnnotation;
import com.example.finalproject.controller.request.LoginRequestDto;
import com.example.finalproject.controller.request.MemberRequestDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RequiredArgsConstructor
@RequestMapping("/lier")
@Controller
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<PrivateResponseBody> signup(
            @RequestBody MemberRequestDto memberRequestDto){
        return memberService.signup(memberRequestDto);
    }


    //로그인 API
    @PostMapping(value = "/login")
    public ResponseEntity<PrivateResponseBody> login(
            @RequestBody LoginRequestDto requestDto,
            HttpServletResponse response) {
        return memberService.login(requestDto, response);
    }

    //로그아웃 API
    @SwaggerAnnotation
    @PostMapping(value = "/logout")
    public ResponseEntity<PrivateResponseBody> logout(
            HttpServletRequest request) {
        return memberService.logout(request);
    }


}
