package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

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

    @Column
    private LocalDateTime starttime;

    @Column
    private LocalDateTime endplaytime;

    @Column
    private Long playhour;

    @Column
    private Long playminute;

    @OneToOne(fetch = FetchType.LAZY)
    private Member member;
}
