package com.example.finalproject.domain;

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
public class MemberActive {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long memberactiveId;

    @Column(nullable = false)
    private Long createNum;

    @Column(nullable = false)
    private Long ownerNum;

    @Column(nullable = false)
    private Long enterNum;

    @Column(nullable = false)
    private Long exitNum;

    @Column(nullable = false)
    private Long gamestartNum;

    @Column(nullable = false)
    private Long gamereadyNum;

    @Column(nullable = false)
    private Long voteNum;

    @Column(nullable = false)
    private Long correctanswerNum;

    @OneToOne(fetch = FetchType.LAZY)
    private Member member;
}
