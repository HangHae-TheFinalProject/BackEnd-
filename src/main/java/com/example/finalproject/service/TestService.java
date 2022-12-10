package com.example.finalproject.service;

import com.example.finalproject.controller.response.GameRoomResponseDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.ChatRoomRepository;
import com.example.finalproject.repository.DynamicQueryDsl;
import com.example.finalproject.repository.GameRoomMemberRepository;
import com.example.finalproject.repository.GameRoomRepository;
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
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;
import static com.example.finalproject.domain.QMemberActive.memberActive;
import static com.example.finalproject.domain.QGameStartSet.gameStartSet;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestService {
    private final DynamicQueryDsl dynamicQueryDsl;
    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomMemberRepository gameRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final EntityManager em;
    private final SimpMessageSendingOperations messagingTemplate;

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

    @Transactional
    public ResponseEntity<?> enterGameRoom(
            Long roomId, // 게임방 id
            HttpServletRequest request) { // 인증정보를 가진 request

        // 토큰 유효성 검증
        Member auth_member = authorizeToken(request);

        // 최종적으로 결과를 보여줄 DTO
        GameRoomResponseDto gameRoomResponseDto;

        // 입장하고자하는 방의 정보 불러오기
        GameRoom enterGameRoom = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(roomId))
//                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 이미 게임방 상태가 시작 중이라면 참가할 수 없음
        if (enterGameRoom.getStatus().equals("start")) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.ALREADY_PLAYING, null), HttpStatus.BAD_REQUEST);
        }

//        // 만약, 관리 DB(GameRoomMember)에 현재 입장하고자 하는 멤버와 입장하고자 하는 방 정보가 매핑이 되어있으면 이미 참가가 되어있는 것이므로 에러 출력
//        if (jpaQueryFactory
//                .selectFrom(gameRoomMember)
//                .where(gameRoomMember.member.eq(auth_member).and(gameRoomMember.gameRoom.eq(enterGameRoom)))
//                .fetchOne() != null) {
//
//            // 이미 참가한 멤버 이슈 출력
//            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.MEMBER_DUPLICATED, null), HttpStatus.BAD_REQUEST);
//        }

        // 현재 입장하고자하는 게임방의 정보를 가지고있는 관리DB(GameRoomMember) 정보들을 리스트화하여 불러오기 (게임 방 정원 확인을 위한 용도)
//        List<GameRoomMember> gameRoomMemberList = jpaQueryFactory
//                .selectFrom(gameRoomMember)
//                .where(gameRoomMember.gameRoom.eq(enterGameRoom))
////                .setLockMode(LockModeType.PESSIMISTIC_READ)
//                .fetch();

//        // 만약, 위에서 불러온 관리DB 정보가 8명이거나 이상이라면 정원 초과로 판단하여 에러 출력
//        if (gameRoomMemberList.size() > 7) {
//
//            // 정원이 초과하여 입장할 수 없다는 이슈 출력
//            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.CANT_ENTER, null), HttpStatus.BAD_REQUEST);
//        }

        // 8명이거나 이하라면, 관리DB (GameRoomMember)에 저장 및 등록 (멤버 게임방 입장)
        // 사실상 어떤 방에 어떤 멤버가 있는지 관리할 수 있는 것은 GameRoomMember 이다.
        GameRoomMember addGameRoomMember1 = GameRoomMember.builder()
                .gameRoom(enterGameRoom)
                .member(auth_member)
                .member_id(auth_member.getMemberId())
                .gameroom_id(enterGameRoom.getRoomId())
                .ready("unready")
                .session("")
                .build();

        // 입장한 정보 저장
        gameRoomMemberRepository.save(addGameRoomMember1);

        // 최종적으로 출력될 DTO에 현재 게임방의 정보와 리스트에 담아온 참가 멤버들의 정보를 input 한다.
        gameRoomResponseDto = GameRoomResponseDto.builder()
                .id(enterGameRoom.getRoomId()) // 입장한 게임방 id
                .roomName(enterGameRoom.getRoomName()) // 입장한 게임방 이름
                .roomPassword(enterGameRoom.getRoomPassword()) // 입장한 게임방 패스워드
                .mode(enterGameRoom.getMode()) // 입장한 게임 모드
                .owner(enterGameRoom.getOwner()) // 입장한 게임방의 방장
                .status(enterGameRoom.getStatus()) // 게임방 상태
                .build();

        // 입장 알림 문구와 참가한 유저수를 저장하기 위한 hashmap (게임 시작 시 인원 제한을 위한 용도)
        HashMap<String, Object> contentset = new HashMap<>();

        // STomp로 입장한 메세지 전달
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(roomId));
        gameMessage.setSenderId(Long.toString(auth_member.getMemberId()));
        gameMessage.setSender(auth_member.getNickname());

        Long memberCnt = jpaQueryFactory
                .select(gameRoomMember.count())
                .from(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(enterGameRoom.getRoomId()))
//                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        contentset.put("owner", enterGameRoom.getOwner());
        contentset.put("memberCnt", memberCnt);
        contentset.put("enterMent", gameMessage.getRoomId() + "번방에 " + gameMessage.getSender() + "님이 입장하셨습니다.");

        gameMessage.setContent(contentset);
        gameMessage.setType(GameMessage.MessageType.JOIN);

        // 구독 주소에 어떤 유저가 집입했는지 메세지 전달 (구독한 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage);

        // 유저의 활동이력 정보 조회
        MemberActive userActive = jpaQueryFactory
                .selectFrom(memberActive)
                .where(memberActive.member.eq(auth_member))
//                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        // 유저가 방에 입장한 이력 업데이트
        jpaQueryFactory
                .update(memberActive)
                .set(memberActive.enterNum, userActive.getEnterNum() + 1L)
                .where(memberActive.member.eq(auth_member))
                .execute();

        em.flush();
        em.clear();

        // 결과 출력
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameRoomResponseDto), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> roomExit(
            Long roomId, // 나가고자 하는 방 id
            HttpServletRequest request) { // 인증 정보를 갖고있는 request

        // 토큰 유효성 검증
        Member auth_member = authorizeToken(request);

        // 나가고자 하는 방의 정보 불러오기
        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(roomId))
//                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        // 나간 후의 게임방 현재 인원 수 확인을 위해 관리DB에 남아있는 멤버들 리스트화
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameRoom.eq(gameRoom1))
                .fetch();

        int remainmembercnt = gameRoomMembers.size();

        int memberlocate = remainmembercnt - 1;

        jpaQueryFactory
                .delete(gameRoomMember)
                .where(gameRoomMember.gameRoomMemberId.eq(gameRoomMembers.get(memberlocate).getGameRoomMemberId()).and(gameRoomMember.gameRoom.eq(gameRoom1)))
                .execute();

//        // 관리DB에서 나가고자하는 게임방에서 참가하고있는 멤버 삭제
//        jpaQueryFactory
//                .delete(gameRoomMember)
//                .where(gameRoomMember.member.eq(auth_member).and(gameRoomMember.gameRoom.eq(gameRoom1)))
//                .execute();
//
//        // 나간 후의 게임방 현재 인원 수 확인을 위해 관리DB에 남아있는 멤버들 리스트화
//        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
//                .selectFrom(gameRoomMember)
//                .where(gameRoomMember.gameRoom.eq(gameRoom1))
////                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
//                .fetch();

        // 만약 멤버가 나간 후, 게임방에 남아있는 멤버가 존재하지 않을 경우에 게임방도 같이 삭제
        if (gameRoomMembers.isEmpty()) {
            jpaQueryFactory
                    .delete(gameRoom)
                    .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                    .execute();

            // 채팅방 삭제
            chatRoomRepository.deleteRoom(gameRoom1.getRoomId().toString());
        }

        // 방을 나갈 경우의 알림 문구와 나간 이후의 방 인원 수를 저장허기 위한 hashmap
        HashMap<String, Object> contentset = new HashMap<>();

        // 누가 방을 나갔는지 소켓으로 전체 공유
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(gameRoom1.getRoomId())); // 퇴장한 방의 id
        gameMessage.setSenderId(Long.toString(auth_member.getMemberId())); //퇴장한 사람 id
        gameMessage.setSender(auth_member.getNickname()); // 퇴장한 사람

        Long memberCnt = jpaQueryFactory
                .select(gameRoomMember.count())
                .from(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameRoom1.getRoomId()))
//                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        contentset.put("memberCnt", memberCnt);
        contentset.put("outMent", gameMessage.getSender() + "님이 방을 나가셨습니다.");

        gameMessage.setContent(contentset); //퇴장 내용
        gameMessage.setType(GameMessage.MessageType.LEAVE); // 메세지 타입

        // 구독자들에게 퇴장 메세지 전달
        messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage);

        // 유저의 활동이력 정보 조회
//        MemberActive userActive = jpaQueryFactory
//                .selectFrom(memberActive)
//                .where(memberActive.member.eq(auth_member))
////                .setLockMode(LockModeType.PESSIMISTIC_READ)
//                .fetchOne();
//
//        // 유저가 방에 입장한 이력 업데이트
//        jpaQueryFactory
//                .update(memberActive)
//                .set(memberActive.exitNum, userActive.getExitNum() + 1L)
//                .where(memberActive.member.eq(auth_member))
//                .execute();

//        // 방에서 나가려고 하는 멤버가 현재 방장이고, 게임방에 남아있는 인원이 존재할 경우에 남은 사람듣 중에 방장을 랜덤으로 지정
//        if (auth_member.getNickname().equals(gameRoom1.getOwner()) && !gameRoomMembers.isEmpty()) {
//
//            // 남은 사람들의 수 만큼 랜덤으로 돌려서 나온 멤버 id
//            Long nextOwnerId = gameRoomMembers.get((int) (Math.random() * gameRoomMembers.size())).getMember_id();
//
//            // 랜덤으로 지정된 멤버 소환
//            Member nextOwner = jpaQueryFactory
//                    .selectFrom(QMember.member)
//                    .where(QMember.member.memberId.eq(nextOwnerId))
////                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
//                    .fetchOne();
//
//            // 다음 방장으로 지정된 유저의 활동이력 정보 조회
//            MemberActive nextOwnerActive = jpaQueryFactory
//                    .selectFrom(memberActive)
//                    .where(memberActive.member.eq(nextOwner))
////                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
//                    .fetchOne();
//
//            // 다음 방장으로 지정된 유저의 owner 이력 업데이트
//            jpaQueryFactory
//                    .update(memberActive)
//                    .set(memberActive.ownerNum, nextOwnerActive.getOwnerNum() + 1L)
//                    .where(memberActive.member.eq(nextOwner))
//                    .execute();
//
//            // 게임 방의 방장 정보 수정
//            jpaQueryFactory
//                    .update(gameRoom)
//                    .set(gameRoom.owner, nextOwner.getNickname())
//                    .where(gameRoom.roomId.eq(roomId))
//                    .execute();
//
//            GameMessage gameMessage1 = new GameMessage<>();
//            gameMessage1.setRoomId(Long.toString(gameRoom1.getRoomId())); // 방 id
//            gameMessage1.setSenderId(Long.toString(nextOwner.getMemberId())); // 다음 방장이 된 유저 id
//            gameMessage1.setSender(nextOwner.getNickname()); // 다음 방장이 된 유저의 닉네임
//            gameMessage1.setContent(gameMessage1.getSender() + "님이 방장이 되셨습니다."); // 새로운 방장 선언
//            gameMessage1.setType(GameMessage.MessageType.NEWOWNER); // 메세지 타입
//
//            messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage1);
//
//        }

        // 게임이 시작된 방에서 나갈 경우
        if (gameRoom1.getStatus().equals("start")) {
            // 해당 게임방 게임세트를 조회
            GameStartSet gameSet = jpaQueryFactory
                    .selectFrom(gameStartSet)
                    .where(gameStartSet.roomId.eq(gameRoom1.getRoomId()))
//                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .fetchOne();

            // 게임세트의 라이어와 나가고자하는 유저의 닉네임이 같다면
            if (gameSet.getLier().equals(auth_member.getNickname())) {

                // StartSet 삭제(초기화)
                jpaQueryFactory
                        .delete(gameStartSet)
                        .where(gameStartSet.gamestartsetId.eq(gameSet.getGamestartsetId()))
                        .execute();

                // 방의 상태 초기화
                jpaQueryFactory
                        .update(gameRoom)
                        .set(gameRoom.status, "wait")
                        .execute();

                // 해당 게임방에 남아있는 유저들의 상태 unready로 초기화
                for (GameRoomMember gameRoomMember1 : gameRoomMembers) {
                    jpaQueryFactory
                            .update(gameRoomMember)
                            .set(gameRoomMember.ready, "unready")
                            .where(gameRoomMember.gameRoomMemberId.eq(gameRoomMember1.getGameRoomMemberId()))
                            .execute();
                }

                GameMessage gameMessage1 = new GameMessage<>();
                gameMessage1.setRoomId(Long.toString(gameRoom1.getRoomId())); // 방 id
                gameMessage1.setSenderId("");
                gameMessage1.setSender("운영자");
                gameMessage1.setContent("라이어 유저의 퇴장으로 인해 게임 준비 단계로 돌아갑니다."); // 라이어 퇴장으로 인한 메세지 내용
                gameMessage1.setType(GameMessage.MessageType.RESET); // 메세지 타입

                messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage1);
            }
        }

        em.flush();
        em.clear();

        // 정상적으로 방을 나가면 문구 출력
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "방을 나가셨습니다."), HttpStatus.OK);
    }
}
