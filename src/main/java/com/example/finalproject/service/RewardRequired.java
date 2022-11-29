package com.example.finalproject.service;

import com.example.finalproject.domain.GameMessage;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Reward;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static com.example.finalproject.domain.QReward.reward;
import static com.example.finalproject.domain.QMember.member;
import static com.example.finalproject.domain.QGameStartSet.gameStartSet;

@RequiredArgsConstructor
@Service
public class RewardRequired implements RewardRequiredInter {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager em;
    private final SimpMessageSendingOperations messagingTemplate;

    // 게임 플레이 시 얻는 업적
    @Override
    public void achievePlayReward(Member playingMember, Long gameroomid) {

        GameMessage gameMessage = new GameMessage();
        List<Reward> rewardlist = new ArrayList<>();

        if (!playingMember.getRewards().isEmpty()) {
            rewardlist = playingMember.getRewards();
        }

        // 게임 전체 첫 번쨰 승리의 경우
        if (playingMember.getWinNum() == 1L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(1L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }

        }


        // 시민으로써 5승 했을 경우 업적
        if (playingMember.getWinCITIZEN() == 5L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(10L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }

        // 라이어로써 5승 했을 경우 업적
        if (playingMember.getWinLIER() == 5L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(11L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }


        // 승리의 기쁨 업적 : 라이어, 시민 총합 10회 승리 시 획득
        if (playingMember.getWinNum() == 10L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(3L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }


        // 빛의 속도 업적 : 1라운드 이내에 라이어든 시민이든 게임이 끝났을경우 획득
        Integer round = jpaQueryFactory
                .select(gameStartSet.round)
                .from(gameStartSet)
                .where(gameStartSet.roomId.eq(gameroomid))
                .fetchOne();

        if (round <= 1) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(5L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }

        // 라이어 헌터 업적 : 시민으로써 10번 승리 시 획득
        if (playingMember.getWinCITIZEN() == 10L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(4L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }


        // 999 : 9번 이상 플레이하고 9번 승리, 9번 패배 시 획득
        if (playingMember.getWinNum() + playingMember.getLossNum() >= 9L && playingMember.getWinNum() == 9L && playingMember.getLossNum() == 9L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(8L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }


        // 게임 전체 첫 번쨰 패배의 경우
        if (playingMember.getLossNum() == 1L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(9L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }


        // 시민으로써 5연패 했을 경우 업적
        if (playingMember.getLossCITIZEN() == 5L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(12L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }

        // 라이어로써 5연패 했을 경우 업적
        if (playingMember.getLossLIER() == 5L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(13L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }

        // 5회 연패 : 5번 패배 시 획득 (라이어 패배, 시민 패배 합산 기준)
        if (playingMember.getLossNum() == 5L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(2L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }

        // 불굴의 의지 : 12회 이상 패배, 4회 승리 시 획득
        if (playingMember.getLossNum() <= 12L && playingMember.getWinNum() == 4L) {
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(6L))
                    .fetchOne();

            if (!rewardlist.contains(reward1)) {

                rewardlist.add(reward1);

                jpaQueryFactory
                        .update(member)
                        .set(member.rewards, rewardlist)
                        .where(member.memberId.eq(playingMember.getMemberId()))
                        .execute();

                em.flush();
                em.clear();

                gameMessage.setSender("운영자");
                gameMessage.setContent("'" + reward1.getRewardName() + "' 업적 달성!");
                gameMessage.setType(GameMessage.MessageType.REWARD);

                messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid + "/" + playingMember.getNickname(), gameMessage);
            }
        }


    }


}
