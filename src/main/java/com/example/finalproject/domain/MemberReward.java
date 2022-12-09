package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class MemberReward {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long memberrewardId;

    @Column(nullable = false)
    private Long memberid;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Long rewardid;

    @Column(nullable = false)
    private String rewardName;

    @OneToOne
    private Member member;

    @OneToOne
    private Reward reward;


}
