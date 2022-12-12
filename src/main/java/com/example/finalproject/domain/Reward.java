package com.example.finalproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
public class Reward {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long rewardId;

    @Column(nullable = false)
    private String rewardName;

    @Column(nullable = false)
    private String rewardDescription;

    @Column(nullable = false)
    private String mentation;

    @Column(nullable = false)
    private boolean isGold;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Integer level;

    @Column
    private String rewardImg;

    @JsonIgnore
    @JoinColumn(name="memberreward_id")
    @OneToOne
    private MemberReward memberReward;


}
