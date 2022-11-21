package com.example.finalproject.service;

import com.example.finalproject.domain.GameMessage;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Reward;
import com.querydsl.jpa.impl.JPAQueryFactory;

import javax.persistence.EntityManager;
import java.util.List;

import static com.example.finalproject.domain.QReward.reward;
import static com.example.finalproject.domain.QMember.member;

public class RewardRequired implements RewardRequiredInter{

    JPAQueryFactory jpaQueryFactory;
    EntityManager em;

//    @Override
//    public Reward achievePlayReward(){
//        return Reward;
//    }

    @Override
    public void achieveVitoryReward(Member playingMember){

        GameMessage gameMessage = new GameMessage();

        if(playingMember.getWinNum() == 5){
            Reward reward1 = jpaQueryFactory
                    .selectFrom(reward)
                    .where(reward.rewardId.eq(5L))
                    .fetchOne();

            List<Reward> rewardlist = playingMember.getRewards();

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
//            gameMessage.setType("");
        }


    }
}
