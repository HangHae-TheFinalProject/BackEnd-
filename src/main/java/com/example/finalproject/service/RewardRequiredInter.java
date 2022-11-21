package com.example.finalproject.service;


import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.Reward;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;

public interface RewardRequiredInter {

//    public Reward achievePlayReward();

    public void achieveVitoryReward(Member member);

}
