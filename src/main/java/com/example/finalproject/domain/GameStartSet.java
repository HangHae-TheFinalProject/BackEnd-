package com.example.finalproject.domain;

import lombok.*;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
@Setter
public class GameStartSet {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long gamestartsetId;

    @Column
    private String lier;

    @Column
    private String category;

    @Column
    private String keyword;

    @Column
    private Long roomId;

    @Column
    private Integer round;

    @Column(nullable = false)
    private Integer spotnum = 0;

    @Column(nullable = false)
    private GameStartSet.Winner winner;

    // 한 게임에서 현재까지 투표된 사람 수를 count 할 변수
    @Column(nullable = false)
    private Integer voteCnt;
    // 투표를 집계할 Hash Map
    @ElementCollection
    @MapKeyColumn
    @Column
    private Map<String, Integer> voteMap =new HashMap<>();

    public enum Winner {
        DEFAULT, CITIZEN, LIER
    }

    public Integer oneMoerRound(){
        this.round++;
        return this.round;
    }

    public Winner getWinner() {
        return this.winner;
    }

    public Integer addVoteCnt(){
        this.voteCnt++;
        return this.voteCnt;
    }
    public void addVoteHashMap(String name){
        this.voteMap.put(name, this.voteMap.getOrDefault(name, 0) + 1);
    }

    public void clearVote(){
        this.voteCnt =0;
        this.voteMap.clear();
    }

    public void setWinner(GameStartSet.Winner winner){
        this.winner = winner;
    }
}
