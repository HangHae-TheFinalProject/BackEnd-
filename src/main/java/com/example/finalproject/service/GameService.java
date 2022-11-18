package com.example.finalproject.service;

import com.example.finalproject.controller.response.GameStartSetResponseDto;
import com.example.finalproject.domain.*;
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
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final SimpMessageSendingOperations messagingTemplate;
    private final EntityManager em;
    private Integer spotNum = 0;

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
    @Transactional
    public ResponseEntity<?> gameStart(HttpServletRequest request, Long gameroomid){
        // 인증 정보 확인
        Member auth_member = authorizeToken(request);

        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(gameroomid))
                .fetchOne();

        if(!auth_member.getNickname().equals(gameRoom1.getOwner())){
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.UNAUTHORIZE,null),HttpStatus.BAD_REQUEST);
        }

        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.status, "start")
                .where(gameRoom.roomId.eq(gameroomid))
                .execute();

        em.flush();
        em.clear();

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


        // STomp
        // 게임방을 구독한 전체 멤버에게 게임시작 메세지 전달
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId(Long.toString(auth_member.getMemberId()));
        gameMessage.setSender(auth_member.getNickname());
        gameMessage.setContent("게임 시작");
        gameMessage.setType(GameMessage.MessageType.START);

        // 라이어용 메세지, 시민용 메세지
        HashMap<String, String> lierOrTrue = new HashMap<>();
        lierOrTrue.put("lier", "라이어");
        lierOrTrue.put("true", gameStartSet.getKeyword());

        // 구독 주소에 어떤 유저가 진입했는지 메세지 전달 (방 안에 있는 구독자 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

//        if(){
//
//        }
        messagingTemplate.convertAndSend("/sub/game/" + gameroomid, lierOrTrue.get("true"));
        messagingTemplate.convertAndSend("/sub/game/lier" + gameroomid, lierOrTrue.get("lier"));

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameStartSetResponseDto), HttpStatus.OK);
    }


    // 스포트라이트
    public ResponseEntity<?> spotlight(Long gameroomid, HttpServletRequest request){

        // 인증 정보가 있어야지 게임 진행
        authorizeToken(request);

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .orderBy(gameRoomMember.createdAt.asc())
                .fetch();

        GameRoomMember nowSpotMember = gameRoomMembers.get(spotNum);

        Member speakNowMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.memberId.eq(nowSpotMember.getMember_id()))
                .fetchOne();

        HashMap<String, String> whoIsNow = new HashMap<>();
        whoIsNow.put("memberId", Long.toString(speakNowMember.getMemberId()));
        whoIsNow.put("email", speakNowMember.getEmail());
        whoIsNow.put("nickname", speakNowMember.getNickname());
        whoIsNow.put("spotNum", Integer.toString(spotNum));

        // STomp로 입장한 메세지 전달
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId(Long.toString(speakNowMember.getMemberId()));
        gameMessage.setSender(speakNowMember.getNickname());
        gameMessage.setContent(gameMessage.getSender() + "님! 발언을 시작해주십시오");
        gameMessage.setType(GameMessage.MessageType.SPOTLIGHT);

        spotNum = spotNum + 1;

        for(int i = 0 ; i < gameRoomMembers.size() ; i++){
            System.out.println("배열 크기 확인 " + i);
        }

        System.out.println("배열 크기 확인 " + gameRoomMembers.size());

        if(spotNum == gameRoomMembers.size()-1){
            spotNum = 0;
            return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "한 바퀴 돌았습니다!"), HttpStatus.OK);
        }

        // 구독 주소에 지금 스포트라이트 받고 있는 사람이 누군지 실시간으로 전달 (방 안에 있는 구독자 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, whoIsNow), HttpStatus.OK);
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    // 게임 시작
    @Transactional
    public ResponseEntity<?> gameStartTest(GameMessage gameMessage, Long gameroomid){
        // 인증 정보 확인
//        Member auth_member = authorizeToken(request);

        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(gameroomid))
                .fetchOne();

//        if(!auth_member.getNickname().equals(gameRoom1.getOwner())){
////            throw new ResponseEntity<>(new PrivateResponseBody(StatusCode.UNAUTHORIZE,null),HttpStatus.BAD_REQUEST);
//        }

        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.status, "start")
                .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                .execute();

        em.flush();
        em.clear();

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


        // STomp
        // 게임방을 구독한 전체 멤버에게 게임시작 메세지 전달
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(Long.toString(gameroomid));
//        gameMessage.setSenderId(Long.toString(auth_member.getMemberId()));
//        gameMessage.setSender(auth_member.getNickname());
//        gameMessage.setContent("게임 시작");
//        gameMessage.setType(GameMessage.MessageType.START);


        // 구독 주소에 어떤 유저가 진입했는지 메세지 전달 (방 안에 있는 구독자 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

//        messagingTemplate.convertAndSend("/sub/game/" + gameroomid, lierOrTrue.get("true"));
//        messagingTemplate.convertAndSend("/sub/game/lier" + gameroomid, lierOrTrue.get("lier"));

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameStartSetResponseDto), HttpStatus.OK);
    }


    // 스포트라이트
    public ResponseEntity<?> spotlightTest(Long gameroomid){

        // 인증 정보가 있어야지 게임 진행
//        authorizeToken(request);

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .orderBy(gameRoomMember.createdAt.asc())
                .fetch();

        GameRoomMember nowSpotMember = gameRoomMembers.get(spotNum);

        Member speakNowMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.memberId.eq(nowSpotMember.getMember_id()))
                .fetchOne();

        HashMap<String, String> whoIsNow = new HashMap<>();
        whoIsNow.put("memberId", Long.toString(speakNowMember.getMemberId()));
        whoIsNow.put("email", speakNowMember.getEmail());
        whoIsNow.put("nickname", speakNowMember.getNickname());
        whoIsNow.put("spotNum", Integer.toString(spotNum));

        // STomp로 입장한 메세지 전달
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId(Long.toString(speakNowMember.getMemberId()));
        gameMessage.setSender(speakNowMember.getNickname());
        gameMessage.setContent(gameMessage.getSender() + "님! 발언을 시작해주십시오");
        gameMessage.setType(GameMessage.MessageType.SPOTLIGHT);

        spotNum = spotNum + 1;

        for(int i = 0 ; i < gameRoomMembers.size() ; i++){
            System.out.println("배열 크기 확인 " + i);
        }

        System.out.println("배열 크기 확인 " + gameRoomMembers.size());

        if(spotNum == gameRoomMembers.size()){
            spotNum = 0;
//            return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "한 바퀴 돌았습니다!"), HttpStatus.OK);
        }

        // 구독 주소에 지금 스포트라이트 받고 있는 사람이 누군지 실시간으로 전달 (방 안에 있는 구독자 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, whoIsNow), HttpStatus.OK);
    }



}
