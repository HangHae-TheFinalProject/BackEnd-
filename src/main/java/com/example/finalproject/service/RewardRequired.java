package com.example.finalproject.service;

import com.example.finalproject.domain.*;
import com.example.finalproject.repository.MemberRepository;
import com.example.finalproject.repository.MemberRewardRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QReward.reward;
import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameStartSet.gameStartSet;
import static com.example.finalproject.domain.QMemberActive.memberActive;
import static com.example.finalproject.domain.QMemberReward.memberReward;

@RequiredArgsConstructor
@Slf4j
@Service
public class RewardRequired implements RewardRequiredInter {

    private final JPAQueryFactory jpaQueryFactory;
    private final MemberRewardRepository memberRewardRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private List<Member> quickWinSet = new ArrayList<>();
    List<MemberReward> rewardlist = new ArrayList<>();

    // 게임 플레이 시 얻는 업적
    @Override
    @Transactional
    public void achievePlayReward(Member playingMember, Long gameroomid) {

        // 업적 획득 시 알림을 보내기 위한 Message
        GameMessage gameMessage = new GameMessage();
        // 이미 얻은 업적인지 확인하기 위한 boolean
        Boolean rewardChecking = false;

        // 유저의 활동 이력 정보 우선 조회
        MemberActive userActive = jpaQueryFactory
                .selectFrom(memberActive)
                .where(memberActive.member.eq(playingMember))
                .fetchOne();

        // 얻은 업적이 존재할 경우
        if (jpaQueryFactory
                .selectFrom(memberReward)
                .where(memberReward.member.eq(playingMember))
                .fetch() != null) {

            // 얻은 업적들을 조회하여 리스트에 저장 (업적을 얻을 수 있는지 없는지 확인하기 위한 용도)
            rewardlist = jpaQueryFactory
                    .selectFrom(memberReward)
                    .where(memberReward.member.eq(playingMember))
                    .fetch();
        }

        // 첫 1승 : 게임 전체 첫 번쨰 승리의 경우
        if (playingMember.getWinNum() == 0) {
            log.info("처음 승리할 시 진입 : {}", playingMember.getWinNum());

            // 첫 1승 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(1L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {
                log.info("얻은 업적이 존재할 경우");

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("첫 1승을 얻는 조건 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }

        }


        // 시민으로써 5승 했을 경우 업적
//        if (playingMember.getWinCITIZEN() == 5L) {
//            Reward reward1 = jpaQueryFactory
//                    .selectFrom(reward)
//                    .where(reward.rewardId.eq(10L))
//                    .fetchOne();
//
//            if (!rewardlist.contains(reward1)) {
//
//                rewardlist.add(reward1);
//
//                jpaQueryFactory
//                        .update(member)
//                        .set(member.rewards, rewardlist)
//                        .where(member.memberId.eq(playingMember.getMemberId()))
//                        .execute();
//
//                em.flush();
//                em.clear();
//
//                gameMessage.setSender("운영자");
//                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
//                gameMessage.setType(GameMessage.MessageType.REWARD);
//
//                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
//            }
//        }

        // 라이어로써 5승 했을 경우 업적
//        if (playingMember.getWinLIER() == 5L) {
//            Reward reward1 = jpaQueryFactory
//                    .selectFrom(reward)
//                    .where(reward.rewardId.eq(11L))
//                    .fetchOne();
//
//            if (!rewardlist.contains(reward1)) {
//
//                rewardlist.add(reward1);
//
//                jpaQueryFactory
//                        .update(member)
//                        .set(member.rewards, rewardlist)
//                        .where(member.memberId.eq(playingMember.getMemberId()))
//                        .execute();
//
//                em.flush();
//                em.clear();
//
//                gameMessage.setSender("운영자");
//                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
//                gameMessage.setType(GameMessage.MessageType.REWARD);
//
//                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
//            }
//        }


        // 승리의 기쁨 업적 : 라이어, 시민 총합 10회 승리 시 획득
        if (playingMember.getWinNum() == 9) {
            log.info("라이어 + 시민으로써 10회 승리한 횟수 : {}", playingMember.getWinNum());

            // 승리의 기쁨 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(3L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("승리의 기쁨 업적을 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }
        }


        // 빛의 속도 업적 : 1라운드 이내에 라이어든 시민이든 게임이 끝났을경우 획득
        // 게임이 끝난 라운드 조회
        Integer round = jpaQueryFactory
                .select(gameStartSet.round)
                .from(gameStartSet)
                .where(gameStartSet.roomId.eq(gameroomid))
                .fetchOne();

        // 1라운드 이내에 끝났을 경우
        if (round <= 1) {
            log.info("빛의 속도로 이긴 라운드");

            // 1라운드 이내로 끝낸 게임 수를 세기위한 integer 타입 변수
            Integer quickWinCnt = 0;

            // 1라운드 이내에 게임을 끝낸 유저를 리스트에 따로 저장
            quickWinSet.add(playingMember);

            // 리스트에 저장된 유저들을 한명씩 조회
            for (Member member2 : quickWinSet) {
                // 방금 막 1라운드 이내에 게임을 끝낸 유저의 닉네임이 저장된 리스트에 존재할 경우 카운트 + 1
                if (member2.getNickname().equals(playingMember.getNickname())) {
                    quickWinCnt = quickWinCnt + 1;
                }
            }

            // 1라운드를 끝낸 수가 2회라면 초기 업적 조건 달성
            if (quickWinCnt == 2) {
                // 빛의 속도 업적 불러오기
                Reward reward1 = jpaQueryFactory
                        .selectFrom(reward)
                        .where(reward.rewardId.eq(5L))
                        .fetchOne();

                //얻은 업적들이 존재할 경우
                if (!rewardlist.isEmpty()) {

                    // 얻은 업적들을 하나씩 조회하여 확인
                    for (MemberReward rewardCheck : rewardlist) {
                        log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                        // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                        if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                            rewardChecking = true;
                        }
                    }
                }

                // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
                if (rewardlist.isEmpty() || rewardChecking == false) {
                    log.info("빛의 속도 업적을 얻는 조건 달성!");

                    // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                    MemberReward userReward = MemberReward.builder()
                            .memberid(playingMember.getMemberId()) // 유저의 id
                            .nickname(playingMember.getNickname()) // 유저의 닉네임
                            .rewardid(reward1.getRewardId()) // 업적 id
                            .rewardName(reward1.getRewardName()) // 업적 이름
                            .member(playingMember) // Member 객체 저장 (예비용)
                            .reward(reward1) // Reward 객체 저장 (예비용)
                            .build();

                    // 관리DB에 저장
                    memberRewardRepository.save(userReward);

                    HashMap<String, String> contentset = new HashMap<>();
                    contentset.put("rewardName",reward1.getRewardName());
                    contentset.put("mentation",reward1.getMentation());

                    // 운영자 측에서 업적을 얻었다고 울림
                    gameMessage.setSender("운영자");
                    // 어떠한 업적을 얻었는지에 대한 내용 포함
                    gameMessage.setContent(contentset);
                    // 메세지 타입은 Reward
                    gameMessage.setType(GameMessage.MessageType.REWARD);

                    // 구독된 주소로 알림
                    messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                    log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                    log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
                }
            }

        }

        // 라이어 헌터 업적 : 시민으로써 10번 승리 시 획득
        if (playingMember.getWinCITIZEN() == 9) {
            log.info("시민으로써 이긴 횟수 : {}", playingMember.getWinCITIZEN());

            // 라이어 헌터 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(4L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("라이어 헌터 업적을 얻는 조건 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }
        }


        // 999 : 9번 이상 플레이하고 9번 승리, 9번 패배 시 획득
        if (playingMember.getWinNum() + playingMember.getLossNum() >= 9 && playingMember.getWinNum() == 8 && playingMember.getLossNum() == 8) {

            // 999 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(8L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("999 업적을 얻는 조건 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }
        }


        // 게임 전체 첫 번쨰 패배의 경우
//        if (playingMember.getLossNum() == 1L) {
//            Reward reward1 = jpaQueryFactory
//                    .selectFrom(reward)
//                    .where(reward.rewardId.eq(9L))
//                    .fetchOne();
//
//            if (!rewardlist.contains(reward1)) {
//
//                rewardlist.add(reward1);
//
//                jpaQueryFactory
//                        .update(member)
//                        .set(member.rewards, rewardlist)
//                        .where(member.memberId.eq(playingMember.getMemberId()))
//                        .execute();
//
//                em.flush();
//                em.clear();
//
//                gameMessage.setSender("운영자");
//                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
//                gameMessage.setType(GameMessage.MessageType.REWARD);
//
//                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
//            }
//        }


        // 시민으로써 5연패 했을 경우 업적
//        if (playingMember.getLossCITIZEN() == 5L) {
//            Reward reward1 = jpaQueryFactory
//                    .selectFrom(reward)
//                    .where(reward.rewardId.eq(12L))
//                    .fetchOne();
//
//            if (!rewardlist.contains(reward1)) {
//
//                rewardlist.add(reward1);
//
//                jpaQueryFactory
//                        .update(member)
//                        .set(member.rewards, rewardlist)
//                        .where(member.memberId.eq(playingMember.getMemberId()))
//                        .execute();
//
//                em.flush();
//                em.clear();
//
//                gameMessage.setSender("운영자");
//                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
//                gameMessage.setType(GameMessage.MessageType.REWARD);
//
//                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
//            }
//        }

        // 라이어로써 5연패 했을 경우 업적
//        if (playingMember.getLossLIER() == 5L) {
//            Reward reward1 = jpaQueryFactory
//                    .selectFrom(reward)
//                    .where(reward.rewardId.eq(13L))
//                    .fetchOne();
//
//            if (!rewardlist.contains(reward1)) {
//
//                rewardlist.add(reward1);
//
//                jpaQueryFactory
//                        .update(member)
//                        .set(member.rewards, rewardlist)
//                        .where(member.memberId.eq(playingMember.getMemberId()))
//                        .execute();
//
//                em.flush();
//                em.clear();
//
//                gameMessage.setSender("운영자");
//                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
//                gameMessage.setType(GameMessage.MessageType.REWARD);
//
//                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
//            }
//        }

        // 5회 연패 : 5번 패배 시 획득 (라이어 패배, 시민 패배 합산 기준)
        if (playingMember.getLossNum() == 4) {

            // 5회 연패 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(2L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("5회 연패 업적을 얻는 조건 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }
        }

        // 불굴의 의지 : 12회 이상 패배, 4회 승리 시 획득
        if (playingMember.getLossNum() >= 11 && playingMember.getWinNum() == 3) {

            // 불굴의 의지 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(6L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("불굴의 의지 업적을 얻는 조건 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }
        }


        // 신과 함께 : 라이어가 정답을 10회 맞춰서 승리했을 시 획득
        if (userActive.getCorrectanswerNum() == 9) {

            // 신과 함께 업적 불러오기
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(7L))
                    .fetchOne();

            //얻은 업적들이 존재할 경우
            if (!rewardlist.isEmpty()) {

                // 얻은 업적들을 하나씩 조회하여 확인
                for (MemberReward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    // 얻으려고 하는 업적이 이미 얻어졌을 경우 boolean을 true로 바꿈 (이미 얻었으므로 얻지 못하게 하기위함)
                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            // 얻은 업적이 하나도 없을 경우 / 이미 얻은 업적이 아닐 경우 (업적 획득)
            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("신과 함께 업적을 얻는 조건 달성!");

                // 업적과 그 업적을 얻은 유저의 정보를 관리DB (MemberReward)에 저장
                MemberReward userReward = MemberReward.builder()
                        .memberid(playingMember.getMemberId()) // 유저의 id
                        .nickname(playingMember.getNickname()) // 유저의 닉네임
                        .rewardid(reward1.getRewardId()) // 업적 id
                        .rewardName(reward1.getRewardName()) // 업적 이름
                        .member(playingMember) // Member 객체 저장 (예비용)
                        .reward(reward1) // Reward 객체 저장 (예비용)
                        .build();

                // 관리DB에 저장
                memberRewardRepository.save(userReward);

                HashMap<String, String> contentset = new HashMap<>();
                contentset.put("rewardName",reward1.getRewardName());
                contentset.put("mentation",reward1.getMentation());

                // 운영자 측에서 업적을 얻었다고 울림
                gameMessage.setSender("운영자");
                // 어떠한 업적을 얻었는지에 대한 내용 포함
                gameMessage.setContent(contentset);
                // 메세지 타입은 Reward
                gameMessage.setType(GameMessage.MessageType.REWARD);

                // 구독된 주소로 알림
                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", userReward.getRewardName());
            }
        }

    }


}
