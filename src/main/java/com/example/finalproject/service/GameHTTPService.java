package com.example.finalproject.service;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.controller.response.VictoryDto;
import com.example.finalproject.controller.response.VoteDto;
import com.example.finalproject.domain.*;

import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.GameStartSetRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;

import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Comparator;

import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameStartSet.gameStartSet;
import static com.example.finalproject.domain.QMemberActive.memberActive;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameHTTPService {

    private final GameStartSetRepository gameStartSetRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final JPAQueryFactory jpaQueryFactory;
    private final TokenProvider tokenProvider;
    private final RewardRequired rewardRequired;
    private final EntityManager em;
    static int cnt = 0;
    static HashMap<String, Integer> voteHashMap = new HashMap<>();

    @Transactional
    public void vote(Long gameroomid, StringDto stringDto) {
        String name = stringDto.getValue();
        voteHashMap.put(name, voteHashMap.getOrDefault(name, 0) + 1);
        cnt++;

        GameStartSet gameStartSet = gameStartSetRepository.findByRoomId(gameroomid);

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();
        int memberNum = gameRoomMembers.size();
        if (cnt != memberNum) {
            GameMessage<?> gameMessage = new GameMessage<>();
            gameMessage.setRoomId(Long.toString(gameroomid));
            gameMessage.setSenderId("");
            gameMessage.setSender("");
            gameMessage.setContent(null);
            gameMessage.setType(GameMessage.MessageType.CONTINUE);
            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

            return;
        }

        List<String> votedName = sortHash(); // 투표가 끝나면 최다투표자 list 뽑음
        GameMessage<List<String>> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");

        if (votedName.size() == 1) {
            // 라이어가 지목 당했을 때
            if (votedName.get(0).equals(gameStartSet.getLier())) {
                gameMessage.setType(GameMessage.MessageType.LIER);
            }
            // 시민이 지목 당했을 때
            else {
                gameMessage.setType(GameMessage.MessageType.NLIER);
                gameStartSet.setWinner(GameStartSet.Winner.LIER);
            }
        } else {
            // 동점 상황일 때
            if (gameStartSet.getRound() < 4) {
                gameMessage.setType(GameMessage.MessageType.DRAW);
            }
            // 동점 상황, 라운도 종료 됐을 때 (라이어 승리)
            else {
                gameMessage.setType(GameMessage.MessageType.DRAWANDENDGAME);
                gameStartSet.setWinner(GameStartSet.Winner.LIER);
            }
        }
        gameMessage.setContent(votedName);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

    }

    public List<String> sortHash() {
        List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(voteHashMap.entrySet());

        // 비교함수 Comparator를 사용하여 내림 차순으로 정렬
        Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
            // compare로 값을 비교
            public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2) {
                // 내림 차순으로 정렬
                return obj2.getValue().compareTo(obj1.getValue());
            }
        });
        int maxValue = 0;
        for (Entry<String, Integer> entry : list_entries) {
            maxValue = entry.getValue();
            break;
        }
        List<String> nickName = new ArrayList<>();
        for (Entry<String, Integer> entry : list_entries) {
            if (entry.getValue() == maxValue) {
                nickName.add(entry.getKey());
            }
        }
        voteHashMap.clear();
        cnt = 0;
        return nickName;
    }

    @Transactional
    public void isAnswer(Long gameroomid, StringDto stringDto) {
        String answer = stringDto.getValue();
        GameStartSet gameStartSet = gameStartSetRepository.findByRoomId(gameroomid);

        GameMessage<Boolean> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        gameMessage.setContent(gameStartSet.getKeyword().replaceAll(" ", "").equals(answer.replaceAll(" ", "")));
        gameMessage.setType(GameMessage.MessageType.RESULT);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        // 라이어가 정답을 맞추면 라이어 승리
        if (gameStartSet.getKeyword().replaceAll(" ", "").equals(answer.replaceAll(" ", ""))) {
            gameStartSet.setWinner(GameStartSet.Winner.LIER);

            // 정답을 맞춘 라이어의 정보 조회
            Member lier = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.nickname.eq(gameStartSet.getLier()))
                    .fetchOne();

            // 라이어의 활동 이력 정보 조회
            MemberActive userActive = jpaQueryFactory
                    .selectFrom(memberActive)
                    .where(memberActive.member.eq(lier))
                    .fetchOne();

            // 라이어의 정답 맞추기 이력정보 업데이트
            jpaQueryFactory
                    .update(memberActive)
                    .set(memberActive.correctanswerNum, userActive.getCorrectanswerNum() + 1L)
                    .where(memberActive.memberactiveId.eq(userActive.getMemberactiveId()))
                    .execute();

            em.flush();
            em.clear();

        }
        // 틀리면 시민 승리
        else {
            gameStartSet.setWinner(GameStartSet.Winner.CITIZEN);
        }
    }

    @Transactional
    public void endGame(Long gameroomid) {
        LocalDateTime endDateTime = LocalDateTime.of(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonth(),
                LocalDateTime.now().getDayOfMonth(),
                LocalDateTime.now().getHour(),
                LocalDateTime.now().getMinute());

        GameStartSet gameStartSet1 = jpaQueryFactory
                .selectFrom(gameStartSet)
                .where(gameStartSet.roomId.eq(gameroomid))
                .fetchOne();

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();

        List<Member> playingMembers = new ArrayList<>();

        for (GameRoomMember gameRoomMember2 : gameRoomMembers) {

            Member each_member = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember2.getMember_id()))
                    .fetchOne();

            MemberActive userActive = jpaQueryFactory
                    .selectFrom(memberActive)
                    .where(memberActive.member.eq(each_member))
                    .fetchOne();

            Long playhour = ChronoUnit.HOURS.between(userActive.getStarttime(), endDateTime);
            Long playminute = ChronoUnit.MINUTES.between(userActive.getStarttime(), endDateTime);

            jpaQueryFactory
                    .update(memberActive)
                    .set(memberActive.endplaytime, endDateTime)
                    .set(memberActive.playhour, userActive.getPlayhour() + playhour)
                    .set(memberActive.playminute, userActive.getPlayminute() + playminute)
                    .where(memberActive.member.eq(each_member))
                    .execute();

            System.out.println("플레이 시간 : " + userActive.getPlayhour() + " 몇분 플레이 : " + userActive.getPlayminute());

            playingMembers.add(each_member);

//             게임 맴버 상태 ready
            jpaQueryFactory
                    .update(gameRoomMember)
                    .set(gameRoomMember.ready, "unready")
                    .where(gameRoomMember.member_id.eq(gameRoomMember2.getMember_id()))
                    .execute();

        }

        // 전적 계산
        VictoryDto victoryDto = new VictoryDto();

        // 해당 게임의 승자가 라이어일 경우
        if (gameStartSet1.getWinner().equals(GameStartSet.Winner.LIER)) {
            for (Member playingMember : playingMembers) {
                // 라이어는 승리
                if (playingMember.getNickname().equals(gameStartSet1.getLier())) {

                    jpaQueryFactory
                            .update(member)
                            .set(member.winNum, playingMember.getWinNum() + 1)
                            .set(member.winLIER, playingMember.getWinLIER() + 1)
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    victoryDto.getWinner().add(playingMember.getNickname());
                }
                // 시민은 패배
                else {

                    jpaQueryFactory
                            .update(member)
                            .set(member.lossNum, playingMember.getLossNum() + 1)
                            .set(member.lossCITIZEN, playingMember.getLossCITIZEN() + 1)
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    victoryDto.getLoser().add(playingMember.getNickname());
                }

                // 게임 플레이 시 업적 획득
                rewardRequired.achievePlayReward(playingMember, gameroomid);
            }
        }
        // 해당 게임의 승자가 시민일 경우
        else {
            for (Member playingMember : playingMembers) {
                // 라이어는 패배
                if (playingMember.getNickname().equals(gameStartSet1.getLier())) {

                    jpaQueryFactory
                            .update(member)
                            .set(member.lossNum, playingMember.getLossNum() + 1)
                            .set(member.lossLIER, playingMember.getLossLIER() + 1)
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    victoryDto.getLoser().add(playingMember.getNickname());
                }
                // 시민은 승리
                else {
                    jpaQueryFactory
                            .update(member)
                            .set(member.winNum, playingMember.getWinNum() + 1)
                            .set(member.winCITIZEN, playingMember.getWinCITIZEN() + 1)
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    victoryDto.getWinner().add(playingMember.getNickname());
                }

                // 게임 플레이 시 업적 획득
                rewardRequired.achievePlayReward(playingMember, gameroomid);
            }
        }
        GameMessage<VictoryDto> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        gameMessage.setContent(victoryDto);
        gameMessage.setType(GameMessage.MessageType.VICTORY);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        // GameStartSet 삭제
        gameStartSetRepository.delete(gameStartSet1);

        // 게임 룸 상태 wait
//        GameRoom gameRoom1 = jpaQueryFactory
//                .selectFrom(gameRoom)
//                .where(gameRoom.roomId.eq(gameroomid))
//                .fetchOne();
//
//        jpaQueryFactory
//                .update(gameRoom)
//                .set(gameRoom.status, "wait")
//                .where(gameRoom.roomId.eq(gameRoom1.getRoomId()))
//                .execute();

        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.status, "wait")
                .where(gameRoom.roomId.eq(
                        jpaQueryFactory
                                .select(gameRoom.roomId)
                                .from(gameRoom)
                                .where(gameRoom.roomId.eq(gameroomid))
                                .fetchOne()
                ))
                .execute();

        em.flush();
        em.clear();
    }
}