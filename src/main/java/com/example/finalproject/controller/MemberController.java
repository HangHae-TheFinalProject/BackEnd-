package com.example.finalproject.controller;

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

    @PostMapping("/login")
    public ResponseEntity<PrivateResponseBody> login(
            @RequestBody MemberRequestDto memberRequestDto,
            HttpServletResponse response){

        return memberService.login(memberRequestDto, response);
    }

}
