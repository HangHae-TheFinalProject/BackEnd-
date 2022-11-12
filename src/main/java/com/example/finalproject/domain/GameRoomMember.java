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
public class GameRoomMember {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long gameRoomMemberId;

    @Column(nullable = false)
    private Long member_id;

    @Column(nullable = false)
    private Long gameroom_id;

    @JoinColumn(name="gameroom_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameRoom gameRoom;

    @JoinColumn(name="member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
}
