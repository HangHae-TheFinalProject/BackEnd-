package com.example.finalproject.service;

import com.example.finalproject.controller.request.LoginRequestDto;
import com.example.finalproject.controller.request.MemberRequestDto;
import com.example.finalproject.controller.request.TokenDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JPAQueryFactory jpaQueryFactory;
    private final MemberRepository memberRepository;

    public ResponseEntity<PrivateResponseBody> signup(MemberRequestDto memberRequestDto) {

        // 아이디 중복 확인
        if (null != isPresentMember(memberRequestDto.getEmail())) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.DUPLICATED_NICKNAME, null), HttpStatus.BAD_REQUEST);
        }

        // 비밀번호 중복 확인
        if (!memberRequestDto.getPassword().equals(memberRequestDto.getPasswordConfirm())) { // 비밀번호 encode 하기 이전에 비교
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.DUPLICATED_PASSWORD, null), HttpStatus.BAD_REQUEST);
        }

        // 회원 정보 저장
        Member member = Member.builder()
                .email(memberRequestDto.getEmail())
                .password(passwordEncoder.encode(memberRequestDto.getPassword())) // 비밀번호 인코딩하여 저장
                .nickname(memberRequestDto.getNickname() + "#" + Integer.toString((int)(Math.random() * 9999)))
                .build();

        memberRepository.save(member);

        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, "회원가입 성공"), HttpStatus.OK);
    }

    // 로그인
    @Transactional
    public ResponseEntity<PrivateResponseBody> login(LoginRequestDto requestDto, HttpServletResponse response) {

        // 로그인 시도한 이메일 아이디가 존재하는 아이디인지 확인 후 저장
        Member member = isPresentMember(requestDto.getEmail());

        // DB에 해당 아이디를 가진 멤버가 없다면 에러 처리
        if (null == member) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_MEMBER_ID_FAIL, null), HttpStatus.BAD_REQUEST);
        }

        // 로그인 시도한 비밀번호를 인코딩하여 존재하는 비밀번호와 일치하는지 확인
        if (!member.validatePassword(passwordEncoder, requestDto.getPassword())) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_PASSWORD_FAIL, null), HttpStatus.BAD_REQUEST);
        }

        //토큰 지급
        TokenDto tokenDto = tokenProvider.generateTokenDto(member);
        // response에 토큰을 담는다
        tokenToHeaders(tokenDto, response);

        // 전달드릴 내용이 매우 적기 때문에 굳이 Dto를 생성하지 않고 hashmap으로 전달
        HashMap<String, String> login_info = new HashMap<>();
        login_info.put("email", member.getEmail());
        login_info.put("nickname", member.getNickname());

        log.info("액세스 토큰 : {}", response.getHeader("Authorization"));
        log.info("리프레시 토큰 : {}", response.getHeader("Refresh-Token"));

        // Message 및 Status를 Return
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, login_info), HttpStatus.OK);
    }

    //로그아웃
    public ResponseEntity<PrivateResponseBody> logout(HttpServletRequest request) {

        // 토큰 확인
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_WRONG_FORM_JWT_TOKEN, null), HttpStatus.BAD_REQUEST);
        }

        Member member = tokenProvider.getMemberFromAuthentication();

        // 회원 확인
        if (null == member) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_MEMBER_ID_FAIL, null), HttpStatus.NOT_FOUND);
        }

        tokenProvider.deleteRefreshToken(member);

        // Message 및 Status를 Return
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, "로그아웃"), HttpStatus.OK);
    }

    //Email 확인
    @Transactional(readOnly = true)
    public Member isPresentMember(String email) {
        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        return optionalMember.orElse(null);
    }

    //토큰 지급
    public void tokenToHeaders(TokenDto tokenDto, HttpServletResponse response) {
        response.addHeader("Authorization", "Bearer " + tokenDto.getAccessToken());
        response.addHeader("Refresh-Token", tokenDto.getRefreshToken());
        response.addHeader("Access-Token-Expire-Time", tokenDto.getAccessTokenExpiresIn().toString());
    }

}
