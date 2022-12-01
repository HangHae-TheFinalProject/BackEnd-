package com.example.finalproject.service;

import com.example.finalproject.controller.request.LoginRequestDto;
import com.example.finalproject.controller.request.MemberRequestDto;
import com.example.finalproject.controller.request.TokenDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.MemberActive;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.MemberActiveRepository;
import com.example.finalproject.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Optional;

import static com.example.finalproject.domain.QMember.member;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JPAQueryFactory jpaQueryFactory;
    private final MemberRepository memberRepository;
    private final MemberActiveRepository memberActiveRepository;

    // 회원가입
    public ResponseEntity<PrivateResponseBody> signup(MemberRequestDto memberRequestDto) {

        // 아이디 중복 확인
        if (null != isPresentMember(memberRequestDto.getEmail())) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.DUPLICATED_EMAIL, null), HttpStatus.BAD_REQUEST);
        }

        // 비밀번호 중복 확인
        if (!memberRequestDto.getPassword().equals(memberRequestDto.getPasswordConfirm())) { // 비밀번호 encode 하기 이전에 비교
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.DUPLICATED_PASSWORD, null), HttpStatus.BAD_REQUEST);
        }

        // 닉네임 중복 확인
        if (!(jpaQueryFactory
                .selectFrom(member)
                .where(member.nickname.eq(memberRequestDto.getNickname()))
                .fetchOne() == null)) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.DUPLICATED_NICKNAME, null), HttpStatus.BAD_REQUEST);
        }

        // 회원 정보 저장
        Member member = Member.builder()
                .email(memberRequestDto.getEmail()) // 이메일
                .password(passwordEncoder.encode(memberRequestDto.getPassword())) // 비밀번호 인코딩하여 저장
                .nickname(memberRequestDto.getNickname() + "#" + (int) (Math.random() * 9999)) // 닉네임 + 고유 숫자값 부여
                .winNum(0L) // 전체 승리 횟수
                .lossNum(0L) // 전체 패배 횟수
                .winCITIZEN(0L) // 시민으로써 승리한 횟수
                .winLIER(0L) // 라이어로써 승리한 횟수
                .lossCITIZEN(0L) // 시민으로써 패배한 횟수
                .lossLIER(0L) // 라이어로써 패배한 횟수
                .build();

        memberRepository.save(member);

        // 유저 활동 기록 초기화 (업적용)
        MemberActive memberActive = MemberActive.builder()
                .createNum(0L) // 방 생성 횟수
                .ownerNum(0L) // 방장이 된 횟수
                .enterNum(0L) // 방에 들어간 횟수
                .exitNum(0L) // 방을 나간 횟수
                .gamereadyNum(0L) // 게임준비 한 횟수
                .gamestartNum(0L) // 게임시작 한 횟수
                .voteNum(0L) // 투표한 횟수
                .correctanswerNum(0L) // 정답을 맞춘 횟수
                .starttime(null)
                .endplaytime(null)
                .playhour(0L)
                .playminute(0L)
                .member(member)
                .build();

        // 활동 기록 초기화 저장
        memberActiveRepository.save(memberActive);

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
        login_info.put("email", member.getEmail()); // 이메일
        login_info.put("nickname", member.getNickname()); // 닉네임

        log.info("액세스 토큰 : {}", response.getHeader("Authorization"));
        log.info("리프레시 토큰 : {}", response.getHeader("Refresh-Token"));

        // Message 및 Status를 Return
        return new ResponseEntity(new PrivateResponseBody(StatusCode.LOGIN_OK, login_info), HttpStatus.OK);
    }


    //로그아웃
    public ResponseEntity<PrivateResponseBody> logout(HttpServletRequest request) {

        log.info("로그아웃 진입 : {}", request.getHeader("Authorization"));

        // 리프레쉬 토큰 확인
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_WRONG_FORM_JWT_TOKEN, null), HttpStatus.BAD_REQUEST);
        }

        // 현재 로그인한 유저 조회
        Member member = tokenProvider.getMemberFromAuthentication();

        // 회원 확인
        if (null == member) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_MEMBER_ID_FAIL, null), HttpStatus.NOT_FOUND);
        }

        // 리프레쉬 토큰 삭제
        tokenProvider.deleteRefreshToken(member);

        // Message 및 Status를 Return
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, "로그아웃"), HttpStatus.OK);
    }

    // 회원 탈퇴
    public ResponseEntity<PrivateResponseBody> cleansing(HttpServletRequest request) {

        log.info("탈퇴 진입 : {}", request.getHeader("Authorization"));

        // 리프레쉬 토큰 확인
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_WRONG_FORM_JWT_TOKEN, null), HttpStatus.BAD_REQUEST);
        }

        // 현재 로그인한 유저 조회
        Member member = tokenProvider.getMemberFromAuthentication();

        // 회원 확인
        if (null == member) {
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.LOGIN_MEMBER_ID_FAIL, null), HttpStatus.NOT_FOUND);
        }

        // 리프레쉬 토큰 삭제
        tokenProvider.deleteRefreshToken(member);

        // 멤버 삭제
        memberRepository.delete(member);

        // Message 및 Status를 Return
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, "회원 탈퇴 성공"), HttpStatus.OK);
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

        log.info("액세스 토큰 : {} , 리프레시 토큰 : {}", response.getHeader("Authorization"), response.getHeader("Refresh-Token"));

    }


}
