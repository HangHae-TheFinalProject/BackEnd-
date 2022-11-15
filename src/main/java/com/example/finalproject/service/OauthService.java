package com.example.finalproject.service;

import com.example.finalproject.controller.request.TokenDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.SocialLoginType;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.MemberRepository;
import com.example.finalproject.social.SocialOauth;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.json.simple.parser.JSONParser;

//import static com.example.finalproject.domain.QMember.member;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {
    private final List<SocialOauth> socialOauthList;
    private final HttpServletResponse response;
    private final MemberService memberService;
    private final JPAQueryFactory jpaQueryFactory;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;

    @Value("${sns.google.url}")
    private String GOOGLE_SNS_BASE_URL;
    @Value("${sns.google.client.id}")
    private String CLIENT_ID;
    @Value("${sns.google.callback.url}")
    private String REDIRECT_URI;
    @Value("${sns.google.client.secret}")
    private String CLIENT_SECRET;
    @Value("${sns.google.token.url}")
    private String ACCESS_TOKEN_URL;

    public void request(SocialLoginType socialLoginType) {
        SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
        String redirectURL = socialOauth.getOauthRedirectURL();
        try {
            response.sendRedirect(redirectURL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public String requestAccessToken(SocialLoginType socialLoginType, String code) {
//        SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
//        return socialOauth.requestAccessToken(code);
//    }


    public ResponseEntity<?> getGoogleUserInfo(String accessToken, HttpServletResponse response) throws Exception {

        //요청하는 클라이언트마다 가진 정보가 다를 수 있기에 HashMap 선언
        HashMap<String, Object> googleUserInfo = new HashMap<>();

        String reqURL = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken;

        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //요청에 필요한 Header에 포함될 내용
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        log.info("## ResponseCode : {}", responseCode);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line = "";
        String result = "";
        while ((line = br.readLine()) != null) {
            result += line;
        }

        JSONParser parser = new JSONParser();
        log.info("## Result = {}", result);

        JSONObject element = (JSONObject) parser.parse(result);
        String name = (String) element.get("name");
        String email = (String) element.get("email");
        String id = (String) element.get("id");
        id = "GOOGLE_" + id;

        googleUserInfo.put("name", name);
        googleUserInfo.put("email", email);
        googleUserInfo.put("id", id);

        log.info("## Login Controller : {}", googleUserInfo);
        log.info("구글 이메일 : {}", googleUserInfo.get("email"));
        log.info("구글 사용자 이름 : {}", googleUserInfo.get("name"));
        log.info("구글 아이디 : {}", googleUserInfo.get("id"));


        Member login_member = memberService.isPresentMember((String) googleUserInfo.get("email"));

        if (login_member == null) {

            Member member = Member.builder()
                    .email((String) googleUserInfo.get("email"))
                    .password(passwordEncoder.encode((String) googleUserInfo.get("email"))) // 비밀번호 인코딩하여 저장
                    .nickname((String) googleUserInfo.get("name") + "#" + Integer.toString((int) (Math.random() * 9999)))
                    .build();

            memberRepository.save(member);

            //토큰 지급
            TokenDto tokenDto = tokenProvider.generateTokenDto(member);
            // response에 토큰을 담는다
            memberService.tokenToHeaders(tokenDto, response);

            HashMap<String, String> login_info = new HashMap<>();
            login_info.put("email", member.getEmail());
            login_info.put("nickname", member.getNickname());

            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.OK, login_info), HttpStatus.OK);
        }

        TokenDto tokenDto = tokenProvider.generateTokenDto(login_member);
        // response에 토큰을 담는다
        memberService.tokenToHeaders(tokenDto, response);

        HashMap<String, String> login_info = new HashMap<>();
        login_info.put("email", login_member.getEmail());
        login_info.put("nickname", login_member.getNickname());


        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, login_info), HttpStatus.OK);
    }


    private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다."));
    }
}