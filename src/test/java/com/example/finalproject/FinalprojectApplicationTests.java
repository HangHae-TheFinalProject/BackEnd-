package com.example.finalproject;

import com.example.finalproject.domain.GameRoom;
import com.example.finalproject.domain.GameRoomMember;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Mode;
import com.example.finalproject.repository.GameRoomMemberRepository;
import com.example.finalproject.repository.GameRoomRepository;
import com.example.finalproject.service.GameRoomService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.List;

import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;

@SpringBootTest
class FinalprojectApplicationTests {

    @Autowired
    JPAQueryFactory jpaQueryFactory;
    @Autowired
    GameRoomRepository gameRoomRepository;
    @Autowired
    GameRoomMemberRepository gameRoomMemberRepository;

    @Test
    @Transactional
    void contextLoads() {

        GameRoom gameRoom1 = GameRoom.builder()
                .roomName("동시성 테스트 게임방") // 게임방 이름
                .roomPassword("") // 게임방 패스워드
                .mode(Mode.modeName(1)) // 게임 모드
                .owner("무명#4762") // 게임 방장
                .status("wait")
                .build();

        gameRoomRepository.save(gameRoom1);

        List<Member> memberList = jpaQueryFactory
                .selectFrom(member)
                .limit(7)
                .fetch();


        for (Member each_member : memberList) {
            GameRoomMember addGameRoomMember1 = GameRoomMember.builder()
                    .gameRoom(gameRoom1)
                    .member(each_member)
                    .member_id(each_member.getMemberId())
                    .gameroom_id(gameRoom1.getRoomId())
                    .ready("unready")
                    .session("")
                    .build();

            gameRoomMemberRepository.save(addGameRoomMember1);
            System.out.println("방 입장 : " + addGameRoomMember1.getMember().getNickname());
        }

        for (Member each_member : memberList) {

            System.out.println("방 나가기 : " + each_member.getNickname());

            jpaQueryFactory
                    .delete(gameRoomMember)
                    .where(gameRoomMember.member_id.eq(each_member.getMemberId()))
                    .execute();
        }


        List<GameRoomMember> listroommember = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameRoom1.getRoomId()))
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();

        System.out.println("방 나가기 잔여 멤버 확인 테스트 : " + listroommember.size());

    }

}
