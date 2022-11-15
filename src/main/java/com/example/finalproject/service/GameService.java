package com.example.finalproject.service;

import com.example.finalproject.controller.response.GameStartSetResponseDto;
import com.example.finalproject.domain.GameRoomMember;
import com.example.finalproject.domain.GameStartSet;
import com.example.finalproject.domain.Keyword;
import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.GameStartSetRepository;
import com.example.finalproject.repository.KeywordRepository;
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
import static com.example.finalproject.domain.QKeyword.keyword;

@RequiredArgsConstructor
@Service
public class GameService {

    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
    private final KeywordRepository keywordRepository;
    private final GameStartSetRepository gameStartSetRepository;

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

        // 게임 진행 중인 참가자들 중 랜덤으로 라이어 선별
        Member lier = playingMembers.get((int)(Math.random() * playingMembers.size()));

        List<Keyword> keywordList = jpaQueryFactory
                .selectFrom(keyword)
                .fetch();

        // 랜덤으로 걸린 키워드
        Keyword chooseKeyword = keywordList.get((int)(Math.random() * keywordList.size()));

        GameStartSet gameStartSet = GameStartSet.builder()
                .lier(lier.getNickname())
                .category(chooseKeyword.getCategory())
                .keyword(chooseKeyword.getWord())
                .roomId(gameroomid)
                .build();

        gameStartSetRepository.save(gameStartSet);

        GameStartSetResponseDto gameStartSetResponseDto = GameStartSetResponseDto.builder()
                .lier(gameStartSet.getLier())
                .category(gameStartSet.getCategory())
                .keyword(gameStartSet.getKeyword())
                .roomId(gameStartSet.getRoomId())
                .build();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameStartSetResponseDto), HttpStatus.OK);
    }


    // 스포트라이트
    public ResponseEntity<?> spotlight(Long gameroomid, int spotNum,HttpServletRequest request){

        // 인증 정보가 있어야지 게임 진행
        authorizeToken(request);


        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, ""), HttpStatus.OK);
    }
}
