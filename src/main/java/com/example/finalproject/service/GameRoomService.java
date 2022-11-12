package com.example.finalproject.service;

import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.controller.response.GameRoomResponseDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.GameRoomMemberRepository;
import com.example.finalproject.repository.GameRoomRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameRoomService {
    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomMemberRepository gameRoomMemberRepository;
    private final EntityManager em;


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


    // 메인페이지 (방 전체 목록 조회) - json
    public ResponseEntity<?> lierMainPage(HttpServletRequest request) {
        // 합칠 떄 사용
        authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

        List<GameRoom> rooms = jpaQueryFactory
                .selectFrom(gameRoom)
                .fetch();

        List<GameRoomResponseDto> gameroomlist = new ArrayList<>();
        List<Member> memberList = new ArrayList<>();

        for (GameRoom gameRoom1 : rooms) {
            List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                    .selectFrom(gameRoomMember)
                    .where(gameRoomMember.gameroom_id.eq(gameRoom1.getRoomId()))
                    .fetch();

                for(int i = 0 ; i < gameRoomMembers.size() ; i++){
                    Member each_member = jpaQueryFactory
                            .selectFrom(member)
                            .where(member.memberId.eq(gameRoomMembers.get(i).getMember_id()))
                            .fetchOne();

                    memberList.add(each_member);
                }

            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(gameRoom1.getRoomId())
                    .roomName(gameRoom1.getRoomName())
                    .mode(gameRoom1.getMode())
                    .member(memberList)
                    .owner(gameRoom1.getOwner())
                    .password(gameRoom1.getRoomPassword())
                    .build();

            gameroomlist.add(gameRoomResponseDto);
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameroomlist), HttpStatus.OK);
    }


    // 방 생성 - json
    @Transactional
    public ResponseEntity<?> makeGameRoom(
            GameRoomRequestDto gameRoomRequestDto,
            HttpServletRequest request) throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
//        Member test_member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (test_member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

        HashMap<String, String> sessionAndToken = connectOpenvidu();

        log.info("세션 아이디 : {}, 토큰 : {}", sessionAndToken.get("sessionId"), sessionAndToken.get("token"));


        // 게임 방 정보 기입
        GameRoom gameRoom1 = GameRoom.builder()
                .roomName(gameRoomRequestDto.getRoomName())
                .roomPassword(gameRoomRequestDto.getRoomPassword())
                .mode(Mode.modeName(gameRoomRequestDto.getMode()))
                .owner(auth_member.getNickname())
                .build();

        // 게임방 생성 (저장)
        gameRoomRepository.save(gameRoom1);

        GameRoomMember gameRoomMember = GameRoomMember.builder()
                .member_id(auth_member.getMemberId())
                .gameroom_id(gameRoom1.getRoomId())
                .build();

        gameRoomMemberRepository.save(gameRoomMember);

        GameRoom gameRoomNow = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                .fetchOne();

        HashMap<String, String> roomInfo = new HashMap<>();

        roomInfo.put("roomName", gameRoomNow.getRoomName());
        roomInfo.put("roomId", Long.toString(gameRoomNow.getRoomId()));
        roomInfo.put("owner", gameRoomNow.getOwner());
        roomInfo.put("sessionId", sessionAndToken.get("sessionId"));
        roomInfo.put("token", sessionAndToken.get("token"));


        log.info("생성한 방 {}에 입장해있는 유저들 {}", gameRoom1.getRoomId());

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, roomInfo), HttpStatus.OK);

    }


    // 방 입장 - json
    @Transactional
    public ResponseEntity<?> enterGameRoom(
            Long roomId,
            HttpServletRequest request) {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버 (방장이 아닌 새로운 멤버가 들어온다고 가정)
//        Member test_member2 = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(2L))
//                .fetchOne();
//        if (test_member2 == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

        GameRoomResponseDto gameRoomResponseDto;

        if (jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(roomId).and(gameRoomMember.member_id.eq(auth_member.getMemberId())))
                .fetchOne() != null) {
            throw new PrivateException(StatusCode.MEMBER_DUPLICATED);
        }

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(roomId))
                .fetch();

        if (gameRoomMembers.size() >= 9) {
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.CANT_ENTER,null),HttpStatus.BAD_REQUEST);
        }


        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(roomId))
                .fetchOne();

        GameRoomMember gameRoomMember = GameRoomMember.builder()
                .member_id(auth_member.getMemberId())
                .gameroom_id(gameRoom1.getRoomId())
                .build();

        gameRoomMemberRepository.save(gameRoomMember);


        List<GameRoomMember> gameRoomMemberlist = jpaQueryFactory
                .selectFrom(QGameRoomMember.gameRoomMember)
                .where(QGameRoomMember.gameRoomMember.gameroom_id.eq(roomId))
                .fetch();

        List<Member> members = new ArrayList<>();

        for(GameRoomMember gameRoomMember1 : gameRoomMemberlist){
            Member inMember = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember1.getMember_id()))
                    .fetchOne();

            members.add(inMember);
        }

        gameRoomResponseDto = GameRoomResponseDto.builder()
                .id(gameRoom1.getRoomId())
                .roomName(gameRoom1.getRoomName())
                .mode(gameRoom1.getMode())
                .member(members)
                .owner(gameRoom1.getOwner())
                .password(gameRoom1.getRoomPassword())
                .build();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameRoomResponseDto), HttpStatus.OK);
    }


    // 방 나가기
    @Transactional
    public ResponseEntity<?> roomExit(
            Long roomId,
            HttpServletRequest request) {

        // 합칠 떄 사용
        Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버 (방장이 아닌 새로운 멤버가 들어온다고 가정)
//        Member test_member = jpaQueryFactory
//                .selectFrom(QMember.member)
//                .where(QMember.member.id.eq(1L))
//                .fetchOne();
//        if (test_member == null) {
//            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
//        }

        // 현재 나가려고 하는 방의 정보
        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(roomId))
                .fetchOne();

        jpaQueryFactory
                .delete(gameRoomMember)
                .where(gameRoomMember.member_id.eq(auth_member.getMemberId()).and(gameRoomMember.gameroom_id.eq(gameRoom1.getRoomId())))
                .execute();

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(roomId))
                .fetch();

        if(gameRoomMembers.isEmpty()){
            jpaQueryFactory
                    .delete(gameRoom)
                    .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                    .execute();
        }

        // 방에서 나가려고 하는 멤버가 현재 방장이라면 남은 사람듣 중에 방장을 랜덤으로 지정
        if (auth_member.getNickname() == gameRoom1.getOwner()) {
            // 남은 사람들의 수 만큼 랜덤으로 돌려서 나온 멤버 id
            Long next_owner_id = gameRoomMembers.get((int) (Math.random() * gameRoomMembers.size())).getMember_id();

            // 랜덤으로 지정된 멤버 소환
            Member member = jpaQueryFactory
                    .selectFrom(QMember.member)
                    .where(QMember.member.memberId.eq(next_owner_id))
                    .fetchOne();

            // 게임 방의 방장 정보 수정
            jpaQueryFactory
                    .update(gameRoom)
                    .set(gameRoom.owner, member.getNickname())
                    .where(gameRoom.roomId.eq(roomId))
                    .execute();

            em.flush();
            em.clear();
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "방을 나가셨습니다."), HttpStatus.OK);
    }


    // OpenVidu sessionId , token 생성
    public HashMap<String, String> connectOpenvidu() throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException {
//        Member member = authorizeToken(request);

        String OPENVIDU_URL = "https://cheiks.shop";
        String OPENVIDU_SECRET = "MY_SECRET";

        OpenVidu openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
        SessionProperties properties = new SessionProperties.Builder().build();

        Session session = openvidu.createSession(properties);

        ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(OpenViduRole.PUBLISHER)
                .data("user_data")
                .build();
        Connection connection = session.createConnection(connectionProperties);
        String token = connection.getToken();

        log.info("세션 아이디 : {} / 토큰 : {}", session.getSessionId(), token);

        HashMap<String, String> sessionAndToken = new HashMap<>();

        sessionAndToken.put("sessionId", session.getSessionId());
        sessionAndToken.put("token", token);

        return sessionAndToken;

    }

}
