package com.example.finalproject.controller;

import com.example.finalproject.domain.SocialLoginType;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.service.OauthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping(value = "/lier/auth")
@Slf4j
public class OauthController {
    private final OauthService oauthService;

    // 프론트에서 인가코드를 통해 결론적으로 뽑힌 액세스 토큰을 가지고와서 구글에게 다시 한번 사용자 조회를 요청한다.
    @PostMapping("/login")
    public ResponseEntity<?> googleUserCheckTest(HttpServletRequest request, HttpServletResponse response) throws Exception{

        log.info("헤더 : {}",request.getHeader("accesstoken"));
        String accesstoken = request.getHeader("accesstoken");

        return oauthService.getGoogleUserInfo(accesstoken, response);
    }
}