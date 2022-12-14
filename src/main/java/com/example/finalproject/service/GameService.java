package com.example.finalproject.service;

import com.example.finalproject.controller.response.GameStartSetResponseDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.GameStartSetRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;
import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QKeyword.keyword;
import static com.example.finalproject.domain.QMemberActive.memberActive;
import static com.example.finalproject.domain.QGameStartSet.gameStartSet;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {

    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
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
    public ResponseEntity<?> gameStart(GameMessage gameMessage, Long gameroomid) {
        // 게임 시작 시간
        LocalDateTime startDateTime = LocalDateTime.of(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonth(),
                LocalDateTime.now().getDayOfMonth(),
                LocalDateTime.now().getHour(),
                LocalDateTime.now().getMinute());

        // 현재 입장한 게임방의 정보를 가져옴
        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(gameroomid))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 게임 시작은 방장만이 할 수 있음
        if (!gameMessage.getSender().equals(gameRoom1.getOwner())) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.UNAUTHORIZE, null), HttpStatus.BAD_REQUEST);
        }

        // 방장이 된 유저의 활동 이력 업데이트를 위한 정보 유저 정보 조회
        Member owner = jpaQueryFactory
                .selectFrom(member)
                .where(member.nickname.eq(gameRoom1.getOwner()))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 방장이 된 유저의 활동이력 정보 조회
        MemberActive ownerActive = jpaQueryFactory
                .selectFrom(memberActive)
                .where(memberActive.member.eq(owner))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 게임시작은 방장만이 할 수 있으므로 방장이 된 유저의 gamestart 이력을 업데이트
        jpaQueryFactory
                .update(memberActive)
                .set(memberActive.gamestartNum, ownerActive.getGamestartNum() + 1L)
                .where(memberActive.member.eq(owner))
                .execute();

        // 게임방에 입장한 유저들 관리DB(GameRoomMember)에서 가져오기
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetch();

        // 게임방의 상태를 start 상태로 업데이트
        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.status, "start") // "wait" 에서 "start" 상태로
                .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                .execute();

        em.flush();
        em.clear();

        // 입장한 멤버들 중 무작위로 라이어를 고르기 위한 리스트 생성
        List<Member> playingMembers = new ArrayList<>();

        // 라이어 선택용 리스트에 입장 유저들 담기
        for (GameRoomMember gameRoomMember2 : gameRoomMembers) {

            // 각 입장 유저의 정보를 가져옴
            Member each_member = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember2.getMember_id()))
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .fetchOne();

            // 유저의 게임시작시간을 기록
            jpaQueryFactory
                    .update(memberActive)
                    .set(memberActive.starttime, startDateTime)
                    .where(memberActive.member.eq(each_member))
                    .execute();

            em.flush();
            em.clear();

            // 무작위 라이어 선택용 리스트에 추가
            playingMembers.add(each_member);
        }

        // 게임 진행 중인 참가자들 중 랜덤으로 라이어 선별
        Member lier = playingMembers.get((int) (Math.random() * playingMembers.size()));

        // 시민들에게 뿌려지게 될 키워드 전체 목록 불러오기
        List<Keyword> keywordList = jpaQueryFactory
                .selectFrom(keyword)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetch();

        // 랜덤으로 걸린 키워드
        Keyword chooseKeyword = keywordList.get((int) (Math.random() * keywordList.size()));

        // GameStartSet에 해당 방의 라이어가 누구인지, 키워드가 어떤 것인지 저장
        GameStartSet gameStartSet = GameStartSet.builder()
                .lier(lier.getNickname()) // 라이어에 걸린 유저 닉네임
                .category(chooseKeyword.getCategory()) // 키워드 카테고리
                .keyword(chooseKeyword.getWord()) // 키워드
                .roomId(gameroomid) // 게임방 id
                .round(1)
                .voteCnt(0)
                .spotnum(0)
                .winner(GameStartSet.Winner.DEFAULT)
                .build();

        // StartSet 저장
        gameStartSetRepository.save(gameStartSet);

        // http 형식으로 프론트에 전달할 반환값 (예비용)
        GameStartSetResponseDto gameStartSetResponseDto = GameStartSetResponseDto.builder()
                .lier(gameStartSet.getLier())
                .category(gameStartSet.getCategory())
                .keyword(gameStartSet.getKeyword())
                .roomId(gameStartSet.getRoomId())
                .spotnum(gameStartSet.getSpotnum())
                .build();

        // 웹소켓으로 방에 참가한 인원 리스트 전달을 위한 리스트
        List<String> memberset = new ArrayList<>();

        // 닉네임만 필요하기에 닉네임만 담음
        for (Member member : playingMembers) {
            memberset.add(member.getNickname());
        }

        // 웹소켓으로 전달드릴 content 내용
        HashMap<String, Object> startset = new HashMap<>();
        startset.put("lier", gameStartSet.getLier()); // 라이어
        startset.put("category", gameStartSet.getCategory()); // 카테고리
        startset.put("keyword", gameStartSet.getKeyword()); // 키워드
        startset.put("memberlist", memberset); // 방에 존재하는 모든 유저들

        if (gameRoom1.getMode().equals(Mode.일반)) {
            gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
            gameMessage.setSenderId(""); // 준비된 유저의 id
            gameMessage.setSender("운영자"); // 준비된 유저의 닉네임
            gameMessage.setContent(startset); // 준비됐다는 내용
            gameMessage.setType(GameMessage.MessageType.START); // 메세지 타입

        } else if (gameRoom1.getMode().equals(Mode.바보)) {
            // 바보모드에서 라이어에 걸린 유저를 위한 같은 카테고리의 키워드들 리스트화
            keywordList = jpaQueryFactory
                    .selectFrom(keyword)
                    .where(keyword.category.eq(chooseKeyword.getCategory()))
                    .fetch();

            // 해당 키워드 리스트에서 정답 키워드 제외
            keywordList.remove(chooseKeyword);

            // 정답 키워드를 제외한 키워드 리스트 중에서 라이어용 키워드 추출
            Keyword lierkeyword = keywordList.get((int) (Math.random() * keywordList.size()));

            startset.put("liercategory", lierkeyword.getCategory()); // 바보 모드용 라이어 카테고리
            startset.put("lierkeyword", lierkeyword.getWord()); // 바보 모드용 라이어 키워드

            gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
            gameMessage.setSenderId(""); // 준비된 유저의 id
            gameMessage.setSender("운영자"); // 준비된 유저의 닉네임
            gameMessage.setContent(startset); // 준비됐다는 내용
            gameMessage.setType(GameMessage.MessageType.START); // 메세지 타입
        }

        // 게임 시작 알림을 방에 구독이 된 유저들에게 알려줌
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameStartSetResponseDto), HttpStatus.OK);
    }


    // 게임 준비
    @Transactional
    public void gameReady(
            GameMessage gameMessage,
            Long gameroomid) {

        // stomp로 게임 준비하고자 하는 멤버의 정보 불러오기
        Member player = jpaQueryFactory
                .selectFrom(member)
                .where(member.nickname.eq(gameMessage.getSender())) // message에 담겨있는 유저의 닉네임으로 조회
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 게임준비 한 유저의 활동이력 정보를 조회
        MemberActive userActive = jpaQueryFactory
                .selectFrom(memberActive)
                .where(memberActive.member.eq(player))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 게임준비한 유저의 이력을 업데이트
        jpaQueryFactory
                .update(memberActive)
                .set(memberActive.gamereadyNum, userActive.getGamereadyNum() + 1L)
                .where(memberActive.member.eq(player))
                .execute();

        // 현재 게임방에 게임 준비하고자하는 유저의 정보가 매핑되어있는지 확인
        GameRoomMember gameRoomMember1 = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid).and(gameRoomMember.member_id.eq(player.getMemberId())))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

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
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 게임 준비된 유저 상태 메세지 공유
        gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
        gameMessage.setSenderId(Long.toString(readyPlayer.getMemberId())); // 준비된 유저의 id
        gameMessage.setSender(readyPlayer.getNickname()); // 준비된 유저의 닉네임
        gameMessage.setContent(gameMessage.getSender() + "님이 준비완료되었습니다."); // 준비됐다는 내용
        gameMessage.setType(GameMessage.MessageType.READY); // 메세지 타입

        // 구독 주소로 해당 메세지 공유
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        // 준비 상태인 유저 수
        Long readyCnt = jpaQueryFactory
                .select(gameRoomMember.count())
                .from(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid).and(gameRoomMember.ready.eq("ready")))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 방에 참가하고있는 전체 유저 수
        Long roomInMember = jpaQueryFactory
                .select(gameRoomMember.count())
                .from(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 모든 유저가 준비 상태일 경우 ALLREADY 메세지 공유
        if (readyCnt == roomInMember) {
            gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 게임방 id
            gameMessage.setSenderId("");
            gameMessage.setSender("");
            gameMessage.setContent("모든 유저가 준비되었습니다."); // 준비됐다는 내용
            gameMessage.setType(GameMessage.MessageType.ALLREADY); // 메세지 타입

            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);
        }

    }


    // 스포트라이트
    @Transactional
    public void spotlight(Long gameroomid) {

        // 현재 게임방 조회
        GameRoom playRoom = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(gameroomid))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 라이어가 게임 도중 방을 나갔을 경우 초기화가 되기때문에 위치값도 초기화
        if (playRoom.getStatus().equals("wait")) {
            jpaQueryFactory
                    .update(gameStartSet)
                    .set(gameStartSet.spotnum, 0)
                    .where(gameStartSet.roomId.eq(playRoom.getRoomId()))
                    .execute();

            em.flush();
            em.clear();
        }

        // 게임방에 참가하고 있는 유저들 불러오기
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .orderBy(gameRoomMember.createdAt.asc()) // 게임에 참가한 유저의 입장 시간 오름차순 순서대로
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetch();

        // 전역 변수로 초기값 0으로 지정된 위치값 : spotNum
        // spotNum 에 위치한 관리DB 유저 정보 조회
        GameMessage gameMessage = new GameMessage();

        // 현재 게임방 스타트셋 불러오기
        GameStartSet gameStartSet1 = jpaQueryFactory
                .selectFrom(gameStartSet)
                .where(gameStartSet.roomId.eq(playRoom.getRoomId()))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        // 현재 유저의 위치값이 전체 유저들의 수보다 적을 경우
        if (gameStartSet1.getSpotnum() < gameRoomMembers.size()) {

            // 현재 스포트라이트가 켜진 유저
            GameRoomMember nowSpotMember = gameRoomMembers.get(gameStartSet1.getSpotnum());

            // 관리DB 유저 정보로 해당 유저의 상세 정보 조회
            Member speakNowMember = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(nowSpotMember.getMember_id()))
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .fetchOne();

            // STomp로 입장한 메세지 전달
            gameMessage.setRoomId(Long.toString(gameroomid)); // 현재 방 id
            gameMessage.setSenderId(Long.toString(speakNowMember.getMemberId())); // 스포트라이트를 받고있는 유저의 id
            gameMessage.setSender(speakNowMember.getNickname()); // 스포트라이트를 받고있는 유저의 닉네임
            gameMessage.setContent(gameMessage.getSender() + "님! 발언을 시작해주십시오"); // 발언을 시작하라는 내용
            gameMessage.setType(GameMessage.MessageType.SPOTLIGHT); // 메세지 타입

            // 구독 주소에 지금 스포트라이트 받고 있는 사람이 누군지 실시간으로 전달 (방 안에 있는 구독자 유저 전부 메세지 받음)
            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

            // 현 위치의 정보를 메세지화시켜서 전달까지 완료했으면 위치값을 1 증가 시켜서 다음 위치의 유저에게 향할 수 있도록 +1
            jpaQueryFactory
                    .update(gameStartSet)
                    .set(gameStartSet.spotnum, gameStartSet1.getSpotnum() + 1)
                    .where(gameStartSet.roomId.eq(playRoom.getRoomId()))
                    .execute();

            em.flush();
            em.clear();

        } else if (gameStartSet1.getSpotnum() == gameRoomMembers.size()) {
            GameStartSet gameStartSet = jpaQueryFactory
                    .selectFrom(QGameStartSet.gameStartSet)
                    .where(QGameStartSet.gameStartSet.roomId.eq(gameroomid))
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .fetchOne();


            // 한바퀴가 끝나면 라운드 1 증가
            int round = gameStartSet.getRound() + 1;
            System.out.println("현재 끝난 라운드 : " + round);

            // 증가된 라운드와 유저 위치값 초기화 업데이트
            jpaQueryFactory
                    .update(QGameStartSet.gameStartSet)
                    .set(QGameStartSet.gameStartSet.round, round)
                    .set(QGameStartSet.gameStartSet.spotnum, 0)
                    .where(QGameStartSet.gameStartSet.roomId.eq(gameStartSet.getRoomId()))
                    .execute();

            em.flush();
            em.clear();

            if (gameStartSet.getRound() < 3) { // 끝난 라운드가 3 보다 작다면 COMPLETE 메세지 전달

                HashMap<String, Object> contentset = new HashMap<>();
                contentset.put("notice", "Round Over!");
                contentset.put("round", gameStartSet.getRound());

                gameMessage.setRoomId(Long.toString(gameroomid)); // 게임 방 id
                gameMessage.setSenderId(""); // senderId는 딱히 필요없으므로 공백처리
                gameMessage.setSender(""); // sender는 딱히 필요없으므로 공백처리
                gameMessage.setContent(contentset); // 마지막 유저의 위치면 한 바퀴를 돌았다는 것으로 간주
                gameMessage.setType(GameMessage.MessageType.COMPLETE); // 메세지 타입

            } else if (gameStartSet.getRound() == 3) { // 끝난 라운드가 3 이라면 ALLCOMPLETE 메세지 전달
                HashMap<String, Object> contentset = new HashMap<>();
                contentset.put("notice", "All Round Over!");
                contentset.put("round", gameStartSet.getRound());

                gameMessage.setRoomId(Long.toString(gameroomid)); // 게임 방 id
                gameMessage.setSenderId(""); // senderId는 딱히 필요없으므로 공백처리
                gameMessage.setSender(""); // sender는 딱히 필요없으므로 공백처리
                gameMessage.setContent(contentset); // 마지막 유저의 위치면 한 바퀴를 돌았다는 것으로 간주
                gameMessage.setType(GameMessage.MessageType.ALLCOMPLETE); // 메세지 타입
            }

            // 한 바퀴 완료 메세지 공유
            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);
        }
    }

    // 한바퀴 더 혹은 투표하기 알람
    public void oneMoreRoundOrVoteStartAlarm(Long gameroomid, GameMessage gameMessage) {

        if (gameMessage.getType().equals(GameMessage.MessageType.VOTE)) {
            gameMessage.setRoomId(Long.toString(gameroomid)); // 게임 방 id
            gameMessage.setSenderId(""); // senderId는 딱히 필요없으므로 공백처리
            gameMessage.setSender("운영자"); // sender는 딱히 필요없으므로 공백처리
            gameMessage.setContent("방장이 '한 바퀴 더'를 선택하셨습니다"); // 마지막 유저의 위치면 한 바퀴를 돌았다는 것으로 간주
            gameMessage.setType(GameMessage.MessageType.ONEMOREROUND); // 메세지 타입

        } else if (gameMessage.getType().equals(GameMessage.MessageType.ONEMOREROUND)) {
            gameMessage.setRoomId(Long.toString(gameroomid)); // 게임 방 id
            gameMessage.setSenderId(""); // senderId는 딱히 필요없으므로 공백처리
            gameMessage.setSender("운영자"); // sender는 딱히 필요없으므로 공백처리
            gameMessage.setContent("방장이 '투표하기'를 선택하셨습니다."); // 마지막 유저의 위치면 한 바퀴를 돌았다는 것으로 간주
            gameMessage.setType(GameMessage.MessageType.VOTE); // 메세지 타입
        }

        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

    }

}
