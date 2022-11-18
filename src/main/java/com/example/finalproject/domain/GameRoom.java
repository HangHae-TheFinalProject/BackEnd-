package com.example.finalproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class GameRoom extends Timestamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long roomId;

    @Column(nullable = false)
    private String roomName;

    @Column
    private String roomPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String status;


    // 추가
    @JsonIgnore
    @JoinColumn(name = "gameroommember_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameRoomMember gameRoomMember;

}
