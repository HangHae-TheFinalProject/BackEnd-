package com.example.finalproject.service;

import com.example.finalproject.domain.GameRoom;
import com.example.finalproject.domain.GameRoomMember;
import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;
import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QMember.member;

@RequiredArgsConstructor
@Service
public class GameService {

    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;

    // 인증 정보 검증 부분을 한 곳으로 모아놓음
    public Member authorizeToken(HttpServletRequest request) {

        // Access 토큰 유효성 확인
        if (request.getHeader("Authorization") == null) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        // Refresh 토큰 유요성 확인
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        // Access, Refresh 토큰 유효성 검증이 완료되었을 경우 인증된 유저 정보 저장
        Member member = tokenProvider.getMemberFromAuthentication();

        // 인증된 유저 정보 반환
        return member;
    }


    // 게임 시작
    public ResponseEntity<?> gameStart(HttpServletRequest request, Long gameroomid){
        // 인증 정보 확인
        authorizeToken(request);

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();

        List<Member> playingMembers = new ArrayList<>();

        for(GameRoomMember gameRoomMember2 : gameRoomMembers){

            Member each_member = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember2.getMember_id()))
                    .fetchOne();

            playingMembers.add(each_member);
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, ""), HttpStatus.OK);
    }
}
