package com.example.finalproject.service;

import com.example.finalproject.controller.request.GameRoomRequestDto;
import com.example.finalproject.controller.response.GameRoomResponseDto;
import com.example.finalproject.domain.GameRoom;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.QMember;
import com.example.finalproject.domain.Room;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.GameRoomRepository;
import com.example.finalproject.util.Parser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameRoom.gameRoom;

@RequiredArgsConstructor
@Service
public class GameRoomServiceImpl implements GameRoomService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String REDIRECT = "redirect:/";

    private final RoomService roomService;
    private final Parser parser;
    private final TokenProvider tokenProvider;
    private final JPAQueryFactory jpaQueryFactory;
    private final GameRoomRepository gameRoomRepository;
    private EntityManager em;


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


//    @Autowired
//    public GameRoomServiceImpl(final RoomService roomService, final Parser parser) {
//        this.roomService = roomService;
//        this.parser = parser;
//    }

    // 메인 페이지로 이동하면서 modelandview로 데이터를 같이 보냄
    @Override
    public ModelAndView displayMainPage(final Long id, final String uuid) {
        final ModelAndView modelAndView = new ModelAndView("main");
        modelAndView.addObject("id", id); // 방 아이디
        modelAndView.addObject("rooms", roomService.getRooms()); // 생성된 방들
        modelAndView.addObject("uuid", uuid); // 유저아이디

        return modelAndView;
    }


    // 방 생성
    @Override
    public ModelAndView processRoomSelection(final String sid, final String uuid, final BindingResult bindingResult) {
        // 에러가 발생할 경우 에러 처리
        if (bindingResult.hasErrors()) {
            return new ModelAndView(REDIRECT);
        }

        Optional<Long> optionalId = parser.parseId(sid);
        optionalId.ifPresent(id -> Optional.ofNullable(uuid).ifPresent(name -> roomService.addRoom(new Room(id))));

        return this.displayMainPage(optionalId.orElse(null), uuid);
    }


    // 방 입장
    @Override
    public ModelAndView displaySelectedRoom(final String sid, final String uuid) {
        // redirect to main page if provided data is invalid
        ModelAndView modelAndView = new ModelAndView(REDIRECT);

        if (parser.parseId(sid).isPresent()) {
            Room room = roomService.findRoomByStringId(sid).orElse(null);
            if (room != null && uuid != null && !uuid.isEmpty()) {
                logger.debug("User {} is going to join Room #{}", uuid, sid);
                // open the chat room
                modelAndView = new ModelAndView("chat_room", "id", sid);
                modelAndView.addObject("uuid", uuid);
            }
        }

        return modelAndView;
    }

    // 방 나가기
    @Override
    public ModelAndView processRoomExit(final String sid, final String uuid) {
        if (sid != null && uuid != null) {
            logger.debug("User {} has left Room #{}", uuid, sid);
            // implement any logic you need
        }
        return new ModelAndView(REDIRECT);
    }

    @Override
    public ModelAndView requestRandomRoomNumber(final String uuid) {

        return this.displayMainPage(randomValue(), uuid);
    }

    private Long randomValue() {
        return ThreadLocalRandom.current().nextLong(0, 100);
    }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 방에 입장한 멤버들
    List<Member> inMemberList = new ArrayList<>();

    // 메인페이지 (방 전체 목록 조회) - json
    public ResponseEntity<?> lierMainPage(HttpServletRequest request) {
        // 합칠 떄 사용
        //authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
        Member member = jpaQueryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.id.eq(1L))
                .fetchOne();
        if (member == null) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        List<GameRoom> rooms = jpaQueryFactory
                .selectFrom(gameRoom)
                .fetch();

        List<GameRoomResponseDto> gameroomlist = new ArrayList<>();

        for (GameRoom gameRoom1 : rooms) {
            List<Member> members = jpaQueryFactory
                    .selectFrom(QMember.member)
                    .where(QMember.member.gameRoom.eq(gameRoom1))
                    .fetch();

            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(gameRoom1.getId())
                    .roomName(gameRoom1.getRoomName())
                    .mode(gameRoom1.getMode())
                    .member(members)
                    .build();

            gameroomlist.add(gameRoomResponseDto);
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameroomlist), HttpStatus.OK);
    }


    // 방 생성 - json
    public ResponseEntity<?> makeGameRoom(
            GameRoomRequestDto gameRoomRequestDto,
            HttpServletRequest request) {

        // 합칠 떄 사용
        // Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버
        Member test_member = jpaQueryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.id.eq(1L))
                .fetchOne();
        if (test_member == null) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        // 방을 생성한 멤버를 방에 입장 처리
        inMemberList.add(test_member);

        GameRoom gameRoom1 = GameRoom.builder()
                .roomName(gameRoomRequestDto.getRoomName())
                .roomPassword(gameRoomRequestDto.getRoomPassword())
                .mode(gameRoomRequestDto.getMode())
                .owner(test_member.getNickname())
                .members(inMemberList)
                .build();


        gameRoomRepository.save(gameRoom1);

        Optional<Long> optionalId = parser.parseRoomId(gameRoom1.getId());
        // 나중에 auth_member 로 변환
        optionalId.ifPresent(id -> Optional.ofNullable(test_member.getNickname()).ifPresent(name -> roomService.addRoom(new Room(id))));

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameRoom1), HttpStatus.OK);
        //html을 연결했으면 이걸로 테스트
//        return this.displayMainPage(optionalId.orElse(null), test_member.getNickname());
    }


    // 방 입장 - json
    @Transactional
    public ResponseEntity<?> enterGameRoom(
            Long roomId,
            HttpServletRequest request) {

        // 합칠 떄 사용
        // Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버 (방장이 아닌 새로운 멤버가 들어온다고 가정)
        Member test_member2 = jpaQueryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.id.eq(2L))
                .fetchOne();
        if (test_member2 == null) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        GameRoomResponseDto gameRoomResponseDto;

        if (parser.parseRoomId(roomId).isPresent()) {

            if (inMemberList.size() >= 9) {
                throw new PrivateException(StatusCode.CANT_ENTER);
            }

            inMemberList.add(test_member2);

            jpaQueryFactory
                    .update(gameRoom)
                    .set(gameRoom.members, inMemberList)
                    .where(gameRoom.id.eq(roomId))
                    .execute();

            em.flush();
            em.clear();

            GameRoom gameRoom1 = jpaQueryFactory
                    .selectFrom(gameRoom)
                    .where(gameRoom.id.eq(roomId))
                    .fetchOne();

            gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(gameRoom1.getId())
                    .roomName(gameRoom1.getRoomName())
                    .mode(gameRoom1.getMode())
                    .member(gameRoom1.getMembers())
                    .owner(gameRoom1.getOwner())
                    .build();

            logger.debug("User {} is going to join Room #{}", test_member2.getNickname(), roomId);

        } else {
            throw new PrivateException(StatusCode.NOT_FOUND_ROOM);
        }

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, gameRoomResponseDto), HttpStatus.OK);
    }


    // 방 나가기
    @Transactional
    public ResponseEntity<?> roomExit(
            Long roomId,
            HttpServletRequest request) {

        // 합칠 떄 사용
        // Member auth_member = authorizeToken(request);

        // 테스트 시 활용할 임의 멤버 (방장이 아닌 새로운 멤버가 들어온다고 가정)
        Member test_member = jpaQueryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.id.eq(1L))
                .fetchOne();
        if (test_member == null) {
            throw new PrivateException(StatusCode.LOGIN_EXPIRED_JWT_TOKEN);
        }

        // 현재 나가려고 하는 방의 정보
        GameRoom gameRoom1 = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.id.eq(roomId))
                .fetchOne();

        // 방에 입장해 있는 멤버 중 현재 나가려고 하는 멤버 지우기
        inMemberList.remove(test_member);

        // 방에서 나간 멤버들의 id를 리스트업
        List<Long> memberidlist = new ArrayList<>();
        for(Member member1 : inMemberList){
            memberidlist.add(member1.getId());
        }

        // 방에서 나가려고 하는 멤버가 현재 방장이라면 남은 사람듣 중에 방장을 랜덤으로 지정
        if(test_member.getNickname() == gameRoom1.getOwner()){
            // 남은 사람들의 수 만큼 랜덤으로 돌려서 나온 멤버 id
            Long next_owner_id = memberidlist.get((int)(Math.random() * memberidlist.size()));

            // 랜덤으로 지정된 멤버 소환
            Member member = jpaQueryFactory
                    .selectFrom(QMember.member)
                    .where(QMember.member.id.eq(next_owner_id))
                    .fetchOne();

            // 게임 방의 방장 정보 수정
            jpaQueryFactory
                    .update(gameRoom)
                    .set(gameRoom.owner, member.getNickname())
                    .where(gameRoom.id.eq(roomId))
                    .execute();

            em.flush();
            em.clear();
        }

        // 게임방 인원 수 업데이트
        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.members, inMemberList)
                .where(gameRoom.id.eq(roomId))
                .execute();

        em.flush();
        em.clear();

        return new ResponseEntity<>(new PrivateResponseBody<>(StatusCode.OK, "방을 나가셨습니다."), HttpStatus.OK);
    }



}
