package com.example.finalproject.service;

import com.example.finalproject.controller.request.TokenDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.MemberActive;
import com.example.finalproject.domain.SocialLoginType;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.MemberActiveRepository;
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

//import static com.example.finalproject.domain.QMemberA.member;

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
    private final MemberActiveRepository memberActiveRepository;
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

    public ResponseEntity<?> getGoogleUserInfo(String accessToken, HttpServletResponse response) throws Exception {

        //???????????? ????????????????????? ?????? ????????? ?????? ??? ????????? HashMap ??????
        HashMap<String, Object> googleUserInfo = new HashMap<>();

        String reqURL = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken;

        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //????????? ????????? Header??? ????????? ??????
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
        log.info("?????? ????????? : {}", googleUserInfo.get("email"));
        log.info("?????? ????????? ?????? : {}", googleUserInfo.get("name"));
        log.info("?????? ????????? : {}", googleUserInfo.get("id"));


        Member login_member = memberService.isPresentMember((String) googleUserInfo.get("email"));

        if (login_member == null) {

            Member member = Member.builder()
                    .email((String) googleUserInfo.get("email"))
                    .password(passwordEncoder.encode((String) googleUserInfo.get("email"))) // ???????????? ??????????????? ??????
                    .nickname((String) googleUserInfo.get("name") + "#" + Integer.toString((int) (Math.random() * 9999)))
                    .winNum(0L) // ?????? ?????? ??????
                    .lossNum(0L) // ?????? ?????? ??????
                    .winCITIZEN(0L) // ??????????????? ????????? ??????
                    .winLIER(0L) // ??????????????? ????????? ??????
                    .lossCITIZEN(0L) // ??????????????? ????????? ??????
                    .lossLIER(0L) // ??????????????? ????????? ??????
                    .build();

            memberRepository.save(member);

            // ?????? ?????? ?????? ????????? (?????????)
            MemberActive memberActive = MemberActive.builder()
                    .createNum(0L) // ??? ?????? ??????
                    .ownerNum(0L) // ????????? ??? ??????
                    .enterNum(0L) // ?????? ????????? ??????
                    .exitNum(0L) // ?????? ?????? ??????
                    .gamereadyNum(0L) // ???????????? ??? ??????
                    .gamestartNum(0L) // ???????????? ??? ??????
                    .voteNum(0L) // ????????? ??????
                    .correctanswerNum(0L) // ????????? ?????? ??????
                    .starttime(null)
                    .endplaytime(null)
                    .playhour(0L)
                    .playminute(0L)
                    .member(member)
                    .build();

            // ?????? ?????? ????????? ??????
            memberActiveRepository.save(memberActive);

            //?????? ??????
            TokenDto tokenDto = tokenProvider.generateTokenDto(member);
            // response??? ????????? ?????????
            memberService.tokenToHeaders(tokenDto, response);

            HashMap<String, String> login_info = new HashMap<>();
            login_info.put("email", member.getEmail());
            login_info.put("nickname", member.getNickname());

            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.OK, login_info), HttpStatus.OK);
        }

        TokenDto tokenDto = tokenProvider.generateTokenDto(login_member);
        // response??? ????????? ?????????
        memberService.tokenToHeaders(tokenDto, response);

        HashMap<String, String> login_info = new HashMap<>();
        login_info.put("email", login_member.getEmail());
        login_info.put("nickname", login_member.getNickname());


        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.LOGIN_OK, login_info), HttpStatus.OK);
    }


    private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("??? ??? ?????? SocialLoginType ?????????."));
    }
}