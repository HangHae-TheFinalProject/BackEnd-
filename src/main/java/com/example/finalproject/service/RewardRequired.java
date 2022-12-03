package com.example.finalproject.service;

import com.example.finalproject.domain.GameMessage;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.MemberActive;
import com.example.finalproject.domain.Reward;
import com.example.finalproject.repository.MemberRepository;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class RewardRequired implements RewardRequiredInter {

    private final JPAQueryFactory jpaQueryFactory;
    private final MemberRepository memberRepository;
    private final EntityManager em;
    private final SimpMessageSendingOperations messagingTemplate;
    private List<Member> quickWinSet = new ArrayList<>();
    List<Reward> rewardlist = new ArrayList<>();

    // 게임 플레이 시 얻는 업적
    @Override
    @Transactional
    public void achievePlayReward(Member playingMember, Long gameroomid) {

        GameMessage gameMessage = new GameMessage();
        Boolean rewardChecking = false;

        // 유저의 확ㄹ동 이력 정보 우선 조회
        MemberActive userActive = jpaQueryFactory
                .selectFrom(memberActive)
                .where(memberActive.member.eq(playingMember))
                .fetchOne();


        if (!playingMember.getRewards().isEmpty()) {
            rewardlist = playingMember.getRewards();
        }

        // 게임 전체 첫 번쨰 승리의 경우
        if (playingMember.getWinNum() == 1) {
            log.info("처음 승리할 시 진입 : {}", playingMember.getWinNum());

            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(1L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                log.info("얻은 업적이 존재할 경우");

                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("첫 1승을 얻는 조건 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
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
        if (playingMember.getWinNum() == 10) {
            log.info("라이어 + 시민으로써 10회 승리한 횟수 : {}", playingMember.getWinNum());

            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(3L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("승리의 기쁨 업적을 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
            }
        }


        // 빛의 속도 업적 : 1라운드 이내에 라이어든 시민이든 게임이 끝났을경우 획득
        Integer round = jpaQueryFactory
                .select(gameStartSet.round)
                .from(gameStartSet)
                .where(gameStartSet.roomId.eq(gameroomid))
                .fetchOne();

        if (round <= 1) {
            log.info("빛의 속도로 이긴 라운드");

            Integer quickWinCnt = 0;

            quickWinSet.add(playingMember);

            for (Member member2 : quickWinSet) {
                if (member2.getNickname().equals(playingMember.getNickname())) {
                    quickWinCnt = quickWinCnt + 1;
                }
            }

            if (quickWinCnt == 2) {
                Reward reward1 = jpaQueryFactory
                        .selectFrom(reward)
                        .where(reward.rewardId.eq(5L))
                        .fetchOne();

                if (!rewardlist.isEmpty()) {
                    for (Reward rewardCheck : rewardlist) {
                        log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                        if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                            rewardChecking = true;
                        }
                    }
                }

                if (rewardlist.isEmpty() || rewardChecking == false) {
                    log.info("빛의 속도 업적을 얻는 조건 달성!");

                    rewardlist.add(reward1);

                    Member member1 = playingMember;

                    member1.setRewards(rewardlist);
                    memberRepository.save(member1);

                    gameMessage.setSender("운영자");
                    gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                    gameMessage.setType(GameMessage.MessageType.REWARD);

                    messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                    log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                    log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
                }
            }

        }

        // 라이어 헌터 업적 : 시민으로써 10번 승리 시 획득
        if (playingMember.getWinCITIZEN() == 10) {
            log.info("시민으로써 이긴 횟수 : {}", playingMember.getWinCITIZEN());

            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(4L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("라이어 헌터 업적을 얻는 조건 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
            }
        }


        // 999 : 9번 이상 플레이하고 9번 승리, 9번 패배 시 획득
        if (playingMember.getWinNum() + playingMember.getLossNum() >= 9 && playingMember.getWinNum() == 9 && playingMember.getLossNum() == 9) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(8L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("999 업적을 얻는 조건 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
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
        if (playingMember.getLossNum() == 5) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(2L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("5회 연패 업적을 얻는 조건 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
            }
        }

        // 불굴의 의지 : 12회 이상 패배, 4회 승리 시 획득
        if (playingMember.getLossNum() >= 12 && playingMember.getWinNum() == 4) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(6L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("불굴의 의지 업적을 얻는 조건 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
            }
        }


        // 신과 함께 : 라이어가 정답을 10회 맞춰서 승리했을 시 획득
        if (userActive.getCorrectanswerNum() == 10) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(7L))
                    .fetchOne();

            if (!rewardlist.isEmpty()) {
                for (Reward rewardCheck : rewardlist) {
                    log.info("얻은 업적 : {}", rewardCheck.getRewardName());

                    if (rewardCheck.getRewardName().equals(reward1.getRewardName())) {
                        rewardChecking = true;
                    }
                }
            }

            if (rewardlist.isEmpty() || rewardChecking == false) {
                log.info("신과 함께 업적을 얻는 조건 달성!");

                rewardlist.add(reward1);

                Member member1 = playingMember;

                member1.setRewards(rewardlist);
                memberRepository.save(member1);

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);

                log.info("[{}] 업적 획득 확인", reward1.getRewardName());
                log.info("유저한테 제대로 들어갔는지 확인 : {}", playingMember.getRewards().get(playingMember.getRewards().size()-1).getRewardName());
            }
        }

    }


}
