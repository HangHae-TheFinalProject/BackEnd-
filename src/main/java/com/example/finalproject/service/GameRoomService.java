package com.example.finalproject.service;

import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.controller.request.StringDto;
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
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.security.Principal;
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
    private final DynamicQueryDsl dynamicQueryDsl;
    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomMemberRepository gameRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final EntityManager em;
    private final SimpMessageSendingOperations messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

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



    // 메인페이지 (방 전체 목록 조회) - 페이징 처리 완료
    public ResponseEntity<?> lierMainPage(
            HttpServletRequest request,
            int pageNum,
            String view
    ) { // 인증정보를 가진 request

        // 토큰 유효성 검증
        authorizeToken(request);

        // 한 페이지 당 보여지는 방 수 (4개)
        int size = 4;
        // 페이징 처리를 위해 현재 페이지와 보여지는 방 수를 곱해놓는다. (4개의 방 수 중 가장 마지막에 나올 위치값)
        int sizeInPage = pageNum * size;

        // 동적QueryDSL로 생성된 전체 게임방 불러오기
        List<GameRoom> rooms = dynamicQueryDsl.findGameRooms(view);

        // 메인페이지에 보여줄 전체 방과 방의 주인 및 방 참여 인원을 출력하기 위한 리스트
        List<GameRoomResponseDto> gameroomlist = new ArrayList<>();

        // 생성된 전체 방의 한 방씩 조회
        for (GameRoom gameRoom1 : rooms) {
            // 방의 id 와 멤버 id 까지 저장되어있는 GameRoomMember 엔티티를 사용해
            // 현재 조회되고 있는 게임방에 속한 멤버들을 GameRoomMember 에서 찾아서 리스트화
            // # GameRoomMember에는 멤버의 정보가 그대로 전부 들어있는 것이 아니라 id 정도만 존재하기 떄문에
            // 따로 Member 엔티티에서 조회해서 추출해야 한다.
            List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                    .selectFrom(gameRoomMember)
                    .where(gameRoomMember.gameRoom.eq(gameRoom1))
                    .orderBy(gameRoomMember.createdAt.asc())
                    .fetch();

            // 방에 속한 멤버들의 정보들을 저장하기위한 리스트
            List<Member> memberList = new ArrayList<>();

                // 게임방에 참가하고있는 멤버들의 인원 수 만큼 조회
                for(int i = 0 ; i < gameRoomMembers.size() ; i++){

                    // 참가한 멤버마다 정보들을 불러옴
                    Member each_member = jpaQueryFactory
                            .selectFrom(member)
                            .where(member.memberId.eq(gameRoomMembers.get(i).getMember_id()))
                            .fetchOne();

                    // 리스트에 정보들을 저장
                    memberList.add(each_member);
                }

            // 바로 DB 정보를 결과값으로 보낼 수 없기 때문에 DTO에 한번 더 저장
            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(gameRoom1.getRoomId()) // 게임방 id
                    .roomName(gameRoom1.getRoomName()) // 게임방 이름
                    .roomPassword(gameRoom1.getRoomPassword()) // 게임방 패스워드
                    .mode(gameRoom1.getMode()) // 게임 모드
                    .member(memberList) // 게임에 참가하고있는 멤버들
                    .owner(gameRoom1.getOwner()) // 게임방의 오너
                    .status(gameRoom1.getStatus()) // 게임방 현재 상태
                    .build();

            // DTO에 담긴 정보들을 리스트에 차곡차곡 저장
            gameroomlist.add(gameRoomResponseDto);
        }

        // 페이징 처리 후 4개의 방만을 보여줄 리스트
        List<GameRoomResponseDto> roomsInPage = new ArrayList<>();

        // 페이징 처리 후 나온 페이지에 존재하는 4개의 방을 담는다.
        for(int i = sizeInPage - size ; i < sizeInPage ; i++){

            // 방을 담는다.
            roomsInPage.add(gameroomlist.get(i));

            // 지금 존재하는 전체 방의 개수와 i 값이 같다면 break로 더이상 담지 않고 빠져나온다.
            if(i == gameroomlist.size()-1){
                break;
            }
        }

        // 페이지 수
        int pageCnt = rooms.size() / size;

        // 만약 페이지 수가 size 와 딱 맞아떨어지지 않고 더 많다면 +1을 해준다.
        if(!(rooms.size() % size == 0)){
            pageCnt = pageCnt + 1;
        }

        // page 번호와 페이지에 존재하는 방들을 담기위한 hashmap
        HashMap<String, Object> pageRoomSet = new HashMap<>();

        // 최대 페이지
        pageRoomSet.put("pageCnt",pageCnt);
        // 페이지 안에 있는 방들
        pageRoomSet.put("roomsInPage",roomsInPage);


        // 결과 출력
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, pageRoomSet), HttpStatus.OK);
    }


    // 방 생성 - json
    @Transactional
    public ResponseEntity<?> makeGameRoom(
            GameRoomRequestDto gameRoomRequestDto, // 방 생성을 위해 input 값이 담긴 DTO
            HttpServletRequest request) // 인증정보를 가진 request
            throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException
    {

        // 토큰 유효성 검증
        Member auth_member = authorizeToken(request);

        // OenVIdu 사옹을 위한 sessionId 와 Token을 생성하여 저장한 HashMap
        // 게임 방에서 화상채팅을 이용할 것이기 때문에 필요
        HashMap<String, String> sessionAndToken = connectOpenvidu();

        log.info("세션 아이디 : {}, 토큰 : {}", sessionAndToken.get("sessionId"), sessionAndToken.get("token"));

        if(gameRoomRequestDto.getRoomName().length() >= 11){
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.ROOMNAME_OVER, null), HttpStatus.BAD_REQUEST);
        }
        if(gameRoomRequestDto.getRoomName() == null || gameRoomRequestDto.getRoomName().equals("")){
            return new ResponseEntity<>(new PrivateResponseBody
                    (StatusCode.ROOMNAME_BLANK, null), HttpStatus.BAD_REQUEST);
        }

        // 게임 방 생성을 위한 DTO 정보 기입
        GameRoom gameRoom1 = GameRoom.builder()
                .roomName(gameRoomRequestDto.getRoomName()) // 게임방 이름
                .roomPassword(gameRoomRequestDto.getRoomPassword()) // 게임방 패스워드
                .mode(Mode.modeName(gameRoomRequestDto.getMode())) // 게임 모드
                .owner(auth_member.getNickname()) // 게임 방장
                .status("wait")
                .build();

        // 게임방 생성 (저장)
        gameRoomRepository.save(gameRoom1);

        // 채팅방 생성
        chatRoomService.createChatRoom( gameRoom1.getRoomId().toString(), gameRoom1.getRoomName());

        // 원하는 정보들만 출력될 수 있도록 HashMap을 생성
        HashMap<String, String> roomInfo = new HashMap<>();

        roomInfo.put("roomName", gameRoom1.getRoomName()); // 게임방 이름
        roomInfo.put("roomId", Long.toString(gameRoom1.getRoomId())); // 게임방 id
        roomInfo.put("roomPassword", gameRoom1.getRoomPassword()); // 게임방 패스워드
        roomInfo.put("mode", gameRoom1.getMode().toString()); // 게임 모드
        roomInfo.put("owner", gameRoom1.getOwner()); // 게임 방장
        roomInfo.put("status", gameRoom1.getStatus()); // 게임방 현재 상태
        roomInfo.put("sessionId", sessionAndToken.get("sessionId")); // OpenVidu sessionId
        roomInfo.put("token", sessionAndToken.get("token")); // OpenVidu token


        log.info("방 {}을 생성한 유저 : {}", gameRoom1.getRoomId(), roomInfo.get("owner"));

        // 결과 출력
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, roomInfo), HttpStatus.OK);
    }


    // 방 입장 - json
    @Transactional
    public ResponseEntity<?> enterGameRoom(
            Long roomId, // 게임방 id
            HttpServletRequest request,
            Principal principal) { // 인증정보를 가진 request

        // 토큰 유효성 검증
        Member auth_member = authorizeToken(request);

        // 최종적으로 결과를 보여줄 DTO
        GameRoomResponseDto gameRoomResponseDto;

        // 입장하고자하는 방의 정보 불러오기
        GameRoom enterGameRoom = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(roomId))
                .fetchOne();

        // 이미 게임방 상태가 시작 중이라면 참가할 수 없음
        if(enterGameRoom.getStatus().equals("start")){
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.ALREADY_PLAYING,null),HttpStatus.BAD_REQUEST);
        }

        // 만약, 관리 DB(GameRoomMember)에 현재 입장하고자 하는 멤버와 입장하고자 하는 방 정보가 매핑이 되어있으면 이미 참가가 되어있는 것이므로 에러 출력
        if (jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.member.eq(auth_member).and(gameRoomMember.gameRoom.eq(enterGameRoom)))
                .fetchOne() != null) {

            // 이미 참가한 멤버 이슈 출력
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.MEMBER_DUPLICATED,null),HttpStatus.BAD_REQUEST);
        }

        // 현재 입장하고자하는 게임방의 정보를 가지고있는 관리DB(GameRoomMember) 정보들을 리스트화하여 불러오기 (게임 방 정원 확인을 위한 용도)
        List<GameRoomMember> gameRoomMemberList = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameRoom.eq(enterGameRoom))
                .fetch();

        // 만약, 위에서 불러온 관리DB 정보가 9명이거나 이상이라면 정원 초과로 판단하여 에러 출력
        if (gameRoomMemberList.size() >= 9) {

            // 정원이 초과하여 입장할 수 없다는 이슈 출력
            return new ResponseEntity<>(new PrivateResponseBody(StatusCode.CANT_ENTER,null),HttpStatus.BAD_REQUEST);
        }

        // 8명이거나 이하라면, 관리DB (GameRoomMember)에 저장 및 등록 (멤버 게임방 입장)
        // 사실상 어떤 방에 어떤 멤버가 있는지 관리할 수 있는 것은 GameRoomMember 이다.
        GameRoomMember addGameRoomMember1 = GameRoomMember.builder()
                .gameRoom(enterGameRoom)
                .member(auth_member)
                .member_id(auth_member.getMemberId())
                .gameroom_id(enterGameRoom.getRoomId())
                .ready("unready")
                .session(principal.getName())
                .build();

        // 입장한 정보 저장
        gameRoomMemberRepository.save(addGameRoomMember1);

        // 다시 한번, 입장하고자하는 방에 속한 멤버들의 정보를 불러온다.
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameRoom.eq(enterGameRoom))
                .orderBy(gameRoomMember.createdAt.asc())
                .fetch();

        // 불러온 멤버 정보들을 하나로 담기 위한 리스트
        List<Member> memberList = new ArrayList<>();

        // 리스트에 불러온 멤버의 정보들을 담는다.
        for(GameRoomMember gameRoomMember1 : gameRoomMembers){
            memberList.add(gameRoomMember1.getMember());
        }

        // 최종적으로 출력될 DTO에 현재 게임방의 정보와 리스트에 담아온 참가 멤버들의 정보를 input 한다.
        gameRoomResponseDto = GameRoomResponseDto.builder()
                .id(enterGameRoom.getRoomId()) // 입장한 게임방 id
                .roomName(enterGameRoom.getRoomName()) // 입장한 게임방 이름
                .roomPassword(enterGameRoom.getRoomPassword()) // 입장한 게임방 패스워드
                .mode(enterGameRoom.getMode()) // 입장한 게임 모드
                .owner(enterGameRoom.getOwner()) // 입장한 게임방의 방장
                .status(enterGameRoom.getStatus()) // 게임방 상태
                .member(memberList) // 입장한 멤버들
                .build();


        // STomp로 입장한 메세지 전달
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(roomId));
        gameMessage.setSenderId(Long.toString(auth_member.getMemberId()));
        gameMessage.setSender(auth_member.getNickname());
        gameMessage.setContent(gameMessage.getRoomId() + "번방에 " + gameMessage.getSender() + "님이 입장하셨습니다.");
        gameMessage.setType(GameMessage.MessageType.JOIN);

        // 구독 주소에 어떤 유저가 집입했는지 메세지 전달 (구독한 유저 전부 메세지 받음)
        messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage);

        // 채팅창에 입장 메세지 출력
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRoomId(Long.toString(roomId));
        chatMessage.setType(ChatMessage.MessageType.ENTER);
        chatMessage.setSender(auth_member.getNickname());
        chatMessage.setMessage(auth_member.getNickname().substring(0,auth_member.getNickname().length()-5) + "님이 게임에 참가하셨습니다.");

        redisTemplate.convertAndSend(channelTopic.getTopic(), chatMessage);

        // 결과 출력
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameRoomResponseDto), HttpStatus.OK);
    }


    // 방 나가기
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
                .fetchOne();

        // 관리DB에서 나가고자하는 게임방에서 참가하고있는 멤버 삭제
        jpaQueryFactory
                .delete(gameRoomMember)
                .where(gameRoomMember.member.eq(auth_member).and(gameRoomMember.gameRoom.eq(gameRoom1)))
                .execute();

        // 나간 후의 게임방 현재 인원 수 확인을 위해 관리DB에 남아있는 멤버들 리스트화
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameRoom.eq(gameRoom1))
                .fetch();

        // 만약 멤버가 나간 후, 게임방에 남아있는 멤버가 존재하지 않을 경우에 게임방도 같이 삭제
        if(gameRoomMembers.isEmpty()){
            jpaQueryFactory
                    .delete(gameRoom)
                    .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
                    .execute();

            // 채팅방 삭제
            chatRoomRepository.deleteRoom(gameRoom1.getRoomId().toString());
        }

        // 누가 방을 나갔는지 소켓으로 전체 공유
        GameMessage gameMessage = new GameMessage();
        gameMessage.setRoomId(Long.toString(gameRoom1.getRoomId())); // 퇴장한 방의 id
        gameMessage.setSenderId(Long.toString(auth_member.getMemberId())); //퇴장한 사람 id
        gameMessage.setSender(auth_member.getNickname()); // 퇴장한 사람
        gameMessage.setContent(gameMessage.getSender() + "님이 방을 나가셨습니다."); //퇴장 내용
        gameMessage.setType(GameMessage.MessageType.LEAVE); // 메세지 타입

        // 구독자들에게 퇴장 메세지 전달
        messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage);

        // 방에서 나가려고 하는 멤버가 현재 방장이고, 게임방에 남아있는 인원이 존재할 경우에 남은 사람듣 중에 방장을 랜덤으로 지정
        if (auth_member.getNickname().equals(gameRoom1.getOwner()) && !gameRoomMembers.isEmpty()) {

            // 남은 사람들의 수 만큼 랜덤으로 돌려서 나온 멤버 id
            Long nextOwnerId = gameRoomMembers.get((int) (Math.random() * gameRoomMembers.size())).getMember_id();

            // 랜덤으로 지정된 멤버 소환
            Member nextOwner = jpaQueryFactory
                    .selectFrom(QMember.member)
                    .where(QMember.member.memberId.eq(nextOwnerId))
                    .fetchOne();

            // 게임 방의 방장 정보 수정
            jpaQueryFactory
                    .update(gameRoom)
                    .set(gameRoom.owner, nextOwner.getNickname())
                    .where(gameRoom.roomId.eq(roomId))
                    .execute();

            em.flush();
            em.clear();

            GameMessage gameMessage1 = new GameMessage<>();
            gameMessage1.setRoomId(Long.toString(gameRoom1.getRoomId())); // 방 id
            gameMessage1.setSenderId(Long.toString(nextOwner.getMemberId())); // 다음 방장이 된 유저 id
            gameMessage1.setSender(nextOwner.getNickname()); // 다음 방장이 된 유저의 닉네임
            gameMessage1.setContent(gameMessage1.getSender() + "님이 방장이 되셨습니다."); // 새로운 방장 선언
            gameMessage1.setType(GameMessage.MessageType.NEWOWNER); // 메세지 타입

            messagingTemplate.convertAndSend("/sub/gameroom/" + roomId, gameMessage1);
        }

        // 정상적으로 방을 나가면 문구 출력
        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "방을 나가셨습니다."), HttpStatus.OK);
    }


    // OpenVidu sessionId , token 생성
    public HashMap<String, String> connectOpenvidu() throws io.openvidu.java.client.OpenViduJavaClientException, io.openvidu.java.client.OpenViduHttpException {

        // 1. OpenVIdu(WebRTC)는 EC2 서버를 추가하여 따로 생성하였음.
        // 2. git bash로 리눅스 언어, docker를 사용하여 OpenVidu를 설정 및 구축함

        // 현재 구축해놓은 OpenVidu 서버 주소
        String OPENVIDU_URL = "https://openvidu.haetae.shop";
        // 서버 주소를 사용하기 위한 SECRET키
        String OPENVIDU_SECRET = "MY_SECRET";

        // OpenVidu 객체에 주소와 SECRET키를 넣어 사용 준비
        OpenVidu openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);

        // 화상채팅을 사용하려면 session을 사용하게 되는데, session 설정값을 가져옴.
        SessionProperties properties = new SessionProperties.Builder().build();


        // OpenVidu에 세션 설정값을 input 하여 session 생성
        Session session = openvidu.createSession(properties);

        // 세션에 연결하기위한 설정 값 빌드
        ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(OpenViduRole.PUBLISHER)
                .data("user_data")
                .build();
        // 빌드된 세션 설정 값으로 세션의 연결점(커넥션) 생성
        Connection connection = session.createConnection(connectionProperties);

        // 커넥션을 사용해 token 생성 (session 과 마찬가지로 OpenVidu 컨텐츠를 사용하기 위해 token도 필요함)
        String token = connection.getToken();

        log.info("세션 아이디 : {} / 토큰 : {}", session.getSessionId(), token);

        // sessionId와 token을 저장하기 위한 HashMap 생성
        HashMap<String, String> sessionAndToken = new HashMap<>();

        // sessionId와 token을 HashMap에 저장
        sessionAndToken.put("sessionId", session.getSessionId());
        sessionAndToken.put("token", token);

        return sessionAndToken;

    }




}
