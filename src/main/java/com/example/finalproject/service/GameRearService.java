package com.example.finalproject.service;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.controller.response.VictoryDto;
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
import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.example.finalproject.domain.QGameRoom.gameRoom;
import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;

import java.util.Map.Entry;

import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameStartSet.gameStartSet;
import static com.example.finalproject.domain.QMemberActive.memberActive;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameRearService {

    private final GameStartSetRepository gameStartSetRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final JPAQueryFactory jpaQueryFactory;
    private final RewardRequired rewardRequired;
    private final EntityManager em;


    // 라이어 투표
    @Transactional
    public void vote(Long gameroomid, StringDto stringDto) {
        // 투표할 사람 nickname 저장
        String name = stringDto.getValue();

        // 해당 게임방에 해당하는 gameStartSet을 불러옴
        GameStartSet gameStartSet = gameStartSetRepository.findByRoomId(gameroomid);

        // hash 를 통해 닉네임에 투표 받은 횟수 저장
        // < 닉네임 : 2 > 형식으로 저장됨
        // getOrDefault를 사용하여 처음 투표되는 것이면 0 + 1, 기존에 투표된 값이 있으면 기존 값 + 1을 저장함
        gameStartSet.addVoteHashMap(name);
        // 투표한 명수 count +1
        int cnt = gameStartSet.addVoteCnt();

        // 해당 게임방에 있는 유저 명수를 구함
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();
        int memberNum = gameRoomMembers.size();

        // 아직 투표중이라면
        if (cnt != memberNum) {
            GameMessage<?> gameMessage = new GameMessage<>();
            gameMessage.setRoomId(Long.toString(gameroomid));
            gameMessage.setSenderId("");
            gameMessage.setSender("");
            gameMessage.setContent(null);
            gameMessage.setType(GameMessage.MessageType.CONTINUE); // 투표중이라는 의미의 CONTINUE type의 메세지 뿌려줌
            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

            return; // 아래 로직 (투표 끝났을 때 수횅되는 로직으로 가지 않고 return)
        }

        // 투표가 끝났다면
        List<String> votedName = sortHash(gameStartSet.getVoteMap()); // sortHash 함수를 통해서 최다투표자 list 뽑음
        // 해당 라운드의 투표가 끝났으므로 voteHashMap와 cnt를 초기화 해줌
        gameStartSet.clearVote();

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
                gameStartSet.setWinner(GameStartSet.Winner.LIER); // 해당 게임의 winner 를 Lier로 지정
            }
        } else {
            // 동점 상황일 때
            if (gameStartSet.getRound() < 4) {
                gameMessage.setType(GameMessage.MessageType.DRAW);
            }
            // 동점 상황, 라운도 종료 됐을 때 (라이어 승리)
            else {
                gameMessage.setType(GameMessage.MessageType.DRAWANDENDGAME);
                gameStartSet.setWinner(GameStartSet.Winner.LIER); // 해당 게임의 winner 를 Lier로 지정
            }
        }
        gameMessage.setContent(votedName); // 최다 투표자 list 를 반환함
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

    }

    // 투표 집계 한 HashMap 을 정렬하고 최다투표자 list를 뽐기 위한 함수
    public List<String> sortHash(Map<String, Integer> hashMap) {
        List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(hashMap.entrySet());

        // 비교함수 Comparator를 사용하여 내림 차순으로 정렬
        Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
            // compare로 값을 비교
            public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2) {
                // 내림 차순으로 정렬
                return obj2.getValue().compareTo(obj1.getValue());
            }
        });

        // 정렬이 끝난 후 최다 투표 수 를 저장함
        int maxValue = list_entries.get(0).getValue();

        List<String> nickName = new ArrayList<>();

        // 최다 투표 수와 같은 투표수를 받은 유저들 닉네임을 저장함 (동점자 포함하기 위함)
        for (Entry<String, Integer> entry : list_entries) {
            if (entry.getValue() == maxValue) {
                nickName.add(entry.getKey());
            }
        }

        // 해당 라운드의 투표가 끝났으므로 voteHashMap와 cnt를 초기화 해줌
        return nickName;
    }

    // 라이어가 정답을 맞췄는지 판단 (투표에서 라이어가 지목됐을 때의 다음 상황)
    @Transactional
    public void isAnswer(Long gameroomid, StringDto stringDto) {
        // 라이어가 작성한 정답을 저장함
        String answer = stringDto.getValue();
        // 해당 게임방에 해당하는 gameStartSet을 불러옴
        GameStartSet gameStartSet = gameStartSetRepository.findByRoomId(gameroomid);

        GameMessage<Boolean> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        // 정답 범위를 넓히기 위해 공백을 제거하고 '라이어가 작성한 정답'과 '실제 정답'을 비교함
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
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .fetchOne();

            // 라이어의 활동 이력 정보 조회
            MemberActive userActive = jpaQueryFactory
                    .selectFrom(memberActive)
                    .where(memberActive.member.eq(lier))
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
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

    // 게임 종료
    @Transactional
    public void endGame(Long gameroomid) {
        // 게임 플레이 시간 측정을 위한 게임 종료 시간
        LocalDateTime endDateTime = LocalDateTime.of(
                LocalDateTime.now().getYear(), // 현재 년도
                LocalDateTime.now().getMonth(), // 현재 달
                LocalDateTime.now().getDayOfMonth(), // 현재 일자
                LocalDateTime.now().getHour(), // 현재 시간
                LocalDateTime.now().getMinute()); // 현재 분

        // 해당 게임방에 해당하는 gameStartSet을 불러옴
        GameStartSet gameStartSet1 = jpaQueryFactory
                .selectFrom(gameStartSet)
                .where(gameStartSet.roomId.eq(gameroomid))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne();

        // 해당 게임 방에 있는 GameRoomMember를 불러옴
        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();

        List<Member> playingMembers = new ArrayList<>();

        // 위에서 얻은 GameRoomMember목록을 통해 유저 정보를 불러옴
        for (GameRoomMember gameRoomMember2 : gameRoomMembers) {

            Member each_member = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember2.getMember_id()))
                    .fetchOne();

            // 게임방 참가 유저들의 활동이력들을 조회
            MemberActive userActive = jpaQueryFactory
                    .selectFrom(memberActive)
                    .where(memberActive.member.eq(each_member))
                    .fetchOne();

            // 게임을 시작한 시간과 게임을 종료한 시간의 차를 구함 (ChronoUnit을 사용)
            Long playhour = ChronoUnit.HOURS.between(userActive.getStarttime(), endDateTime);
            // 게임을 시작한 n분과 게임을 종료한 n분의 차를 구함 (ChronoUnit을 사용)
            Long playminute = ChronoUnit.MINUTES.between(userActive.getStarttime(), endDateTime);


            // 각 유저의 게임 종료 시간과 플레이한 시간을 더해서 업데이트
            jpaQueryFactory
                    .update(memberActive)
                    .set(memberActive.endplaytime, endDateTime) // 종료 시간 업데이트
                    .set(memberActive.playhour, userActive.getPlayhour() + playhour) // 플레이한 시간 추가하여 업데이트
                    .set(memberActive.playminute, userActive.getPlayminute() + playminute) // 플레이한 n분 추가하여 업데이트
                    .where(memberActive.member.eq(each_member))
                    .execute();

            System.out.println("플레이 시간 : " + userActive.getPlayhour() + " 몇분 플레이 : " + userActive.getPlayminute());

            // 업데이트한 상태를 저장
            playingMembers.add(each_member);

            // 게임 맴버 상태 unready
            jpaQueryFactory
                    .update(gameRoomMember)
                    .set(gameRoomMember.ready, "unready")
                    .where(gameRoomMember.member_id.eq(gameRoomMember2.getMember_id()))
                    .execute();

            em.flush();
            em.clear();
        }

        // 전적 계산, 업데이트
        // 클라이언트로 승리자, 패배자 list 반환할 Dto
        VictoryDto victoryDto = new VictoryDto();

        // 해당 게임의 승자가 라이어일 경우
        if (gameStartSet1.getWinner().equals(GameStartSet.Winner.LIER)) {
            for (Member playingMember : playingMembers) {
                // 라이어는 승리
                if (playingMember.getNickname().equals(gameStartSet1.getLier())) {

                    jpaQueryFactory
                            .update(member)
                            .set(member.winNum, playingMember.getWinNum() + 1) // 승리 횟수 +1
                            .set(member.winLIER, playingMember.getWinLIER() + 1) // 라이어로 승리 횟수 +1
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    // 승리자 목록에 추가
                    victoryDto.getWinner().add(playingMember.getNickname());
                }
                // 시민은 패배
                else {

                    jpaQueryFactory
                            .update(member)
                            .set(member.lossNum, playingMember.getLossNum() + 1) // 패배 횟수 +1
                            .set(member.lossCITIZEN, playingMember.getLossCITIZEN() + 1) // 시민으로 패배 횟수 +1
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();
                    // 패배자 목록에 추가
                    victoryDto.getLoser().add(playingMember.getNickname());
                }

                // 게임 플레이 시 업적 획득 (인터페이스)
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
                            .set(member.lossNum, playingMember.getLossNum() + 1) // 패배 횟수 +1
                            .set(member.lossLIER, playingMember.getLossLIER() + 1) // 라이아로 패배 횟수 +1
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    // 패배자 목록에 추가
                    victoryDto.getLoser().add(playingMember.getNickname());
                }
                // 시민은 승리
                else {
                    jpaQueryFactory
                            .update(member)
                            .set(member.winNum, playingMember.getWinNum() + 1) // 승리 횟수 +1
                            .set(member.winCITIZEN, playingMember.getWinCITIZEN() + 1) // 시민으로 승리 횟수 +1
                            .where(member.memberId.eq(playingMember.getMemberId()))
                            .execute();

                    // 승리자 목록에 추가
                    victoryDto.getWinner().add(playingMember.getNickname());
                }

                // 게임 플레이 시 업적 획득 (인터페이스)
                rewardRequired.achievePlayReward(playingMember, gameroomid);
            }
        }
        GameMessage<VictoryDto> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        gameMessage.setContent(victoryDto); // 승리자, 패배자 list 메세지 뿌려줌
        gameMessage.setType(GameMessage.MessageType.VICTORY);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        // GameStartSet 삭제
        gameStartSetRepository.delete(gameStartSet1);

        // 게임방 상태를 wait 바꿈 (서브쿼리 사용하여 코드량 단축)
        jpaQueryFactory
                .update(gameRoom)
                .set(gameRoom.status, "wait")
                .where(gameRoom.roomId.eq(
                        jpaQueryFactory // 서브쿼리로 해당 게임방 id를 불러온다.
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