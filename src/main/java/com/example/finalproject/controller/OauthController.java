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

    /**
     * 사용자로부터 SNS 로그인 요청을 Social Login Type 을 받아 처리
     * @param socialLoginType (GOOGLE, FACEBOOK, NAVER, KAKAO)
     */
    @GetMapping(value = "/{socialLoginType}")
    public void socialLoginType(
            @PathVariable(name = "socialLoginType") SocialLoginType socialLoginType) {
        log.info(">> 사용자로부터 SNS 로그인 요청을 받음 :: {} Social Login", socialLoginType);
        oauthService.request(socialLoginType);
    }

    /**
     * Social Login API Server 요청에 의한 callback 을 처리
     * @param socialLoginType (GOOGLE, FACEBOOK, NAVER, KAKAO)
     * @param code API Server 로부터 넘어노는 code
     * @return SNS Login 요청 결과로 받은 Json 형태의 String 문자열 (access_token, refresh_token 등)
     */
//    @GetMapping(value = "/{socialLoginType}/callback")
//    public String callback(
//            @PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
//            @RequestParam(name = "code") String code,
//            HttpServletRequest request, HttpServletResponse response) {
//        log.info(">> 소셜 로그인 API 서버로부터 받은 code :: {}", code);
//        return oauthService.requestAccessToken(socialLoginType, code);
//    }

    @GetMapping(value = "/{socialLoginType}/callback")
    public ResponseEntity<?> callback(
            @PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
            @RequestParam(name = "code") String code,
            HttpServletRequest request, HttpServletResponse response) {
        log.info(">> 소셜 로그인 API 서버로부터 받은 code :: {}", code);

//        String accessToken = oauthService.getGoogleAccessToken(request, response);

//        getGoogleUserInfo(accessToken);

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "구글 사용자 정보 조회 성공"), HttpStatus.OK);
    }


    // 프론트에서 인가코드를 통해 결론적으로 뽑힌 액세스 토큰을 가지고와서 구글에게 다시 한번 사용자 조회를 요청한다.
    @PostMapping("/token/receive")
    public ResponseEntity<?> googleUserCheckTest(HttpServletRequest request) throws Exception{

        log.info("헤더 : {}",request.getHeader("accesstoken"));
        String accesstoken = request.getHeader("accesstoken");
        oauthService.getGoogleUserInfo(accesstoken);

        return null;
    }
}