package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class GameRoomMember extends Timestamped{

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long gameRoomMemberId;

    @Column(nullable = false)
    private Long member_id;

    @Column(nullable = false)
    private Long gameroom_id;

    // 추가
    @JoinColumn(name="gameroomid")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameRoom gameRoom;

    // 추가
    @JoinColumn(name="memberid")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
}
