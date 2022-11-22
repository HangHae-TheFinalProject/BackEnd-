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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    private Integer readyNum = 0;

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
    public ResponseEntity<?> gameStart(GameMessage gameMessage, Long gameroomid) {

        // 현재 입장한 게임방의 정보를 가져옴
        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(gameroomid))
                .fetchOne();

        // 게임 시작은 방장만이 할 수 있음
        if (!gameMessage.getSender().equals(gameRoom1.getOwner())) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.UNAUTHORIZE, null), HttpStatus.BAD_REQUEST);
        }

        // 게임방의 상태를 start 상태로 업데이트
        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.status, "start") // "wait" 에서 "start" 상태로
                .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                .execute();

        em.flush();
        em.clear();

        // 게임방에 입장한 유저들 관리DB(GameRoomMember)에서 가져오기
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();

        // 입장한 멤버들 중 무작위로 라이어를 고르기 위한 리스트 생성
        List<Member> playingMembers = new ArrayList<>();

        // 라이어 선택용 리스트에 입장 유저들 담기
        for (GameRoomMember gameRoomMember2 : gameRoomMembers) {

            // 각 입장 유저의 정보를 가져옴
            Member each_member = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember2.getMember_id()))
                    .fetchOne();

            // 무작위 라이어 선택용 리스트에 추가
            playingMembers.add(each_member);
        }

        // 게임 진행 중인 참가자들 중 랜덤으로 라이어 선별
        Member lier = playingMembers.get((int) (Math.random() * playingMembers.size()));

        // 시민들에게 뿌려지게 될 키워드 전체 목록 불러오기
        List<Keyword> keywordList = jpaQueryFactory
                .selectFrom(keyword)
                .fetch();

        // 랜덤으로 걸린 키워드
        Keyword chooseKeyword = keywordList.get((int) (Math.random() * keywordList.size()));

        // GameStartSet에 해당 방의 라이어가 누구인지, 키워드가 어떤 것인지 저장
        GameStartSet gameStartSet = GameStartSet.builder()
                .lier(lier.getNickname()) // 라이어에 걸린 유저 닉네임
                .category(chooseKeyword.getCategory()) // 키워드 카테고리
                .keyword(chooseKeyword.getWord()) // 키워드
                .roomId(gameroomid) // 게임방 id
                .round(0)
                .winner(null)
                .build();

        // StartSet 저장
        gameStartSetRepository.save(gameStartSet);;

        // http 형식으로 프론트에 전달할 반환값 (예비용)
        GameStartSetResponseDto gameStartSetResponseDto = GameStartSetResponseDto.builder()
                .lier(gameStartSet.getLier())
                .category(gameStartSet.getCategory())
                .keyword(gameStartSet.getKeyword())
                .roomId(gameStartSet.getRoomId())
                .build();


        HashMap<String, String> startset = new HashMap<>();
        startset.put("lier", gameStartSet.getLier());
        startset.put("category", gameStartSet.getCategory());
        startset.put("keyword", gameStartSet.getKeyword());

        gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
        gameMessage.setSenderId(""); // 준비된 유저의 id
        gameMessage.setSender("운영자"); // 준비된 유저의 닉네임
        gameMessage.setContent(startset); // 준비됫다는 내용
        gameMessage.setType(GameMessage.MessageType.START); // 메세지 타입


        // 게임 시작 알림을 방에 구독이 된 유저들에게 알려줌
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

//        List<GameRoomMember> gameRoomMemberlist = jpaQueryFactory
//                .selectFrom(gameRoomMember)
//                .where(gameRoomMember.gameroom_id.eq(gameroomid))
//                .fetch();
//
//        for(GameRoomMember gameRoomMember1 : gameRoomMemberlist){
//            Member roommember = jpaQueryFactory
//                    .selectFrom(member)
//                    .where(member.memberId.eq(gameRoomMember1.getMember_id()))
//                    .fetchOne();
//
//            // 라이어가 아닐 경우
//            if(!roommember.getNickname().equals(gameStartSet.getLier())){
//                log.info("시민에 걸린 유저 : {}", roommember.getNickname());
//
//                // 방 입장했을 떄 각 유저마다의 생성된 session 불러오기
//                String session = jpaQueryFactory
//                        .select(gameRoomMember.session)
//                        .from(gameRoomMember)
//                        .where(gameRoomMember.member_id.eq(roommember.getMemberId()).and(gameRoomMember.gameroom_id.eq(gameroomid)))
//                        .fetchOne();
//
//                gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
//                gameMessage.setSenderId(Long.toString(roommember.getMemberId())); // truer id
//                gameMessage.setSender(roommember.getNickname()); // truer 닉네임
//                gameMessage.setContent(gameMessage.getSender() + "님은 시민입니다. / [" + gameStartSet.getCategory() + " : " + gameStartSet.getKeyword() + "]"); // 시민 여부 내용
//                gameMessage.setType(GameMessage.MessageType.TRUER); // 메세지 타입
//
//                messagingTemplate.convertAndSendToUser(session, "/sub/truer/gameroom/"+gameroomid, gameMessage);
//
//            }else if(roommember.getNickname().equals(gameStartSet.getLier())){ // 라이어일 경우
//                log.info("라이어에 걸린 유저 : {}", gameStartSet.getLier());
//
//                // 방 입장했을 떄 각 유저마다의 생성된 session 불러오기
//                String session = jpaQueryFactory
//                        .select(gameRoomMember.session)
//                        .from(gameRoomMember)
//                        .where(gameRoomMember.member_id.eq(roommember.getMemberId()).and(gameRoomMember.gameroom_id.eq(gameroomid)))
//                        .fetchOne();
//
//                gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
//                gameMessage.setSenderId(Long.toString(roommember.getMemberId())); // lier id
//                gameMessage.setSender(roommember.getNickname()); // lier 닉네임
//                gameMessage.setContent(gameMessage.getSender() + "님은 라이어입니다. / [" + gameStartSet.getCategory() + "]"); // 라이어 여부
//                gameMessage.setType(GameMessage.MessageType.LIAR); // 메세지 타입
//
//                messagingTemplate.convertAndSendToUser(session, "/sub/lier/gameroom/"+gameroomid, gameMessage);
//            }
//        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameStartSetResponseDto), HttpStatus.OK);
    }


    // 게임 준비
    @Transactional
    public ResponseEntity<?> gameReady(
            GameMessage gameMessage,
            Long gameroomid) {

        // stomp로 게임 준비하고자 하는 멤버의 정보 불러오기
        Member player = jpaQueryFactory
                .selectFrom(member)
                .where(member.nickname.eq(gameMessage.getSender())) // message에 담겨있는 유저의 닉네임으로 조회
                .fetchOne();

        // 현재 게임방에 게임 준비하고자하는 유저의 정보가 매핑되어있는지 확인
        GameRoomMember gameRoomMember1 = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid).and(gameRoomMember.member_id.eq(player.getMemberId())))
                .fetchOne();

        // 현재 게임방에 해당 유저가 존재하지않을 경우, 에러 처리
        if (gameRoomMember1 == null) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.NOT_MATCH_PLAYER, null), HttpStatus.BAD_REQUEST);
        }

        // 게임 유저의 준비상태가 unready일 경우
        if (gameRoomMember1.getReady().equals("unready")) {
            // ready 상태로 전환
            jpaQueryFactory
                    .update(gameRoomMember)
                    .set(gameRoomMember.ready, "ready")
                    .where(gameRoomMember.member_id.eq(gameRoomMember1.getMember_id()))
                    .execute();

        } else if (gameRoomMember1.getReady().equals("ready")) { // 게임 유저의 준비 상태가 ready일 경우
            // unready 상태로 전환 (게임 준비 취소)
            jpaQueryFactory
                    .update(gameRoomMember)
                    .set(gameRoomMember.ready, "unready")
                    .where(gameRoomMember.member_id.eq(gameRoomMember1.getMember_id()))
                    .execute();
        }

        em.flush();
        em.clear();

        // 준비된 유저 다시 조회
        Member readyPlayer = jpaQueryFactory
                .selectFrom(member)
                .where(member.memberId.eq(gameRoomMember1.getMember_id()))
                .fetchOne();

        // 게임 준비된 유저 상태 메세지 공유
        gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
        gameMessage.setSenderId(Long.toString(readyPlayer.getMemberId())); // 준비된 유저의 id
        gameMessage.setSender(readyPlayer.getNickname()); // 준비된 유저의 닉네임
        gameMessage.setContent(gameMessage.getSender() + "님이 준비완료되었습니다."); // 준비됫다는 내용
        gameMessage.setType(GameMessage.MessageType.READY); // 메세지 타입

        // 구독 주소로 해당 메세지 공유
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        // 준비 상태인 유저 수
        Long readyCnt = jpaQueryFactory
                .select(gameRoomMember.count())
                .from(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid).and(gameRoomMember.ready.eq("ready")))
                .fetchOne();

        // 방에 참가하고있는 전체 유저 수
        Long roomInMember = jpaQueryFactory
                .select(gameRoomMember.count())
                .from(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetchOne();

        // 모든 유저가 준비 상태일 경우 ALLREADY 메세지 공유
        if (readyCnt == roomInMember) {
            gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
            gameMessage.setSenderId("");
            gameMessage.setSender("");
            gameMessage.setContent("모든 유저가 준비되었습니다."); // 준비됫다는 내용
            gameMessage.setType(GameMessage.MessageType.ALLREADY); // 메세지 타입

            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "준비완료되었습니다."), HttpStatus.OK);
    }


    // 스포트라이트
    @Transactional
    public ResponseEntity<?> spotlight(Long gameroomid) {
        // 게임방에 참가하고 있는 유저들 불러오기
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .orderBy(gameRoomMember.createdAt.asc()) // 게임에 참가한 유저의 입장 시간 오름차순 순서대로
                .fetch();

        // 전역 변수로 초기값 0으로 지정된 위치값 : spotNum
        // spotNum 에 위치한 관리DB 유저 정보 조회
        GameRoomMember nowSpotMember = gameRoomMembers.get(spotNum);

        // 관리DB 유저 정보로 해당 유저의 상세 정보 조회
        Member speakNowMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.memberId.eq(nowSpotMember.getMember_id()))
                .fetchOne();

        // STomp로 입장한 메세지 전달
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 방 id
        gameMessage.setSenderId(Long.toString(speakNowMember.getMemberId())); // 스포트라이트를 받고있는 유저의 id
        gameMessage.setSender(speakNowMember.getNickname()); // 스포트라이트를 받고있는 유저의 닉네임
        gameMessage.setContent(gameMessage.getSender() + "님! 발언을 시작해주십시오"); // 발언을 시작하라는 내용
        gameMessage.setType(GameMessage.MessageType.SPOTLIGHT); // 메세지 타입

        System.out.println("배열 크기 확인 " + gameRoomMembers.size());

        // 구독 주소에 지금 스포트라이트 받고 있는 사람이 누군지 실시간으로 전달 (방 안에 있는 구독자 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);



        GameStartSet gameStartSet = jpaQueryFactory
                .selectFrom(QGameStartSet.gameStartSet)
                .where(QGameStartSet.gameStartSet.roomId.eq(gameroomid))
                .fetchOne();

        // 현재 위치값이 마지막 유저의 위치와 같을 경우
        if (spotNum == gameRoomMembers.size()-1) {
            jpaQueryFactory
                    .update(QGameStartSet.gameStartSet)
                    .set(QGameStartSet.gameStartSet.round, gameStartSet.getRound() + 1)
                    .where(QGameStartSet.gameStartSet.roomId.eq(gameStartSet.getRoomId()))
                    .execute();

            em.flush();
            em.clear();

            if (gameStartSet.getRound() < 3) {
                gameMessage.setRoomId(Long.toString(gameroomid)); // 게임 방 id
                gameMessage.setSenderId(""); // senderId는 딱히 필요없으므로 공백처리
                gameMessage.setSender(""); // sender는 딱히 필요없으므로 공백처리
                gameMessage.setContent("Round Over!"); // 마지막 유저의 위치면 한 바퀴를 돌았다는 것으로 간주
                gameMessage.setType(GameMessage.MessageType.COMPLETE); // 메세지 타입

            }else if (gameStartSet.getRound() == 3) {
                gameMessage.setRoomId(Long.toString(gameroomid)); // 게임 방 id
                gameMessage.setSenderId(""); // senderId는 딱히 필요없으므로 공백처리
                gameMessage.setSender(""); // sender는 딱히 필요없으므로 공백처리
                gameMessage.setContent("All Round Over!"); // 마지막 유저의 위치면 한 바퀴를 돌았다는 것으로 간주
                gameMessage.setType(GameMessage.MessageType.ALLCOMPLETE); // 메세지 타입
            }

            // 한 바퀴 완료 메세지 공유
            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

            // 한 바퀴를 다 돌았으면 위치값을 0으로 초기화
            spotNum = 0;
        }else{

            // 현 위치의 정보를 메세지화시켜서 전달까지 완료했으면 위치값을 1 증가 시켜서 다음 위치의 유저에게 향할 수 있도록 +1
            spotNum = spotNum + 1;
        }

        // http 방식으로 넘겨드릴 현재 스포트라이트를 받고있는 유저의 정보 및 위치값을 hashmap으로 저장
        HashMap<String, String> whoIsNow = new HashMap<>();
        whoIsNow.put("memberId", Long.toString(speakNowMember.getMemberId())); // 스포트라이트를 받은 유저의 id
        whoIsNow.put("email", speakNowMember.getEmail()); // 스포트라이트를 받은 유저의 이메일
        whoIsNow.put("nickname", speakNowMember.getNickname()); // 스포트라이트를 받은 유저의 닉네임
        whoIsNow.put("spotNum", Integer.toString(spotNum)); // 현재 위치값
        whoIsNow.put("round", Integer.toString(gameStartSet.getRound())); // 현재 위치값

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, whoIsNow), HttpStatus.OK);
    }

}
