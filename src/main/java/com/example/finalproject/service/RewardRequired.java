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

@RequiredArgsConstructor
@Service
public class RewardRequired implements RewardRequiredInter{

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager em;
    private final SimpMessageSendingOperations messagingTemplate;

//    @Override
//    public Reward achievePlayReward(){
//        return Reward;
//    }

    // 게임 승리 시 얻는 업적
    @Override
    public void achieveVitoryReward(Member playingMember, Long gameroomid){

        GameMessage gameMessage = new GameMessage();
        List<Reward> rewardlist = new ArrayList<>();

        // 게임 전체 첫 번쨰 승리의 경우
        if(playingMember.getWinNum() == 1){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(3L))
                    .fetchOne();

            if(!playingMember.getRewards().isEmpty()){
                rewardlist = playingMember.getRewards();
            }

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


        // 시민으로써 5연승 했을 경우 업적
        if(playingMember.getWinCITIZEN() == 5){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(5L))
                    .fetchOne();

            if(!playingMember.getRewards().isEmpty()){
                rewardlist = playingMember.getRewards();
            }

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

        // 라이어로써 5연승 했을 경우 업적
        if(playingMember.getWinLIER() == 5){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(4L))
                    .fetchOne();

            if(!playingMember.getRewards().isEmpty()){
                rewardlist = playingMember.getRewards();
            }

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


    // 게임 패배 시 얻는 업적
    @Override
    public void achieveLoseReward(Member playingMember, Long gameroomid){

        GameMessage gameMessage = new GameMessage();
        List<Reward> rewardlist = new ArrayList<>();

        // 게임 전체 첫 번쨰 패배의 경우
        if(playingMember.getLossNum() == 1){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(6L))
                    .fetchOne();

            if(!playingMember.getRewards().isEmpty()){
                rewardlist = playingMember.getRewards();
            }

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


        // 시민으로써 5연패 했을 경우 업적
        if(playingMember.getLossCITIZEN() == 5){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(8L))
                    .fetchOne();

            if(!playingMember.getRewards().isEmpty()){
                rewardlist = playingMember.getRewards();
            }

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

        // 라이어로써 5연패 했을 경우 업적
        if(playingMember.getLossLIER() == 5){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(7L))
                    .fetchOne();

            if(!playingMember.getRewards().isEmpty()){
                rewardlist = playingMember.getRewards();
            }

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
