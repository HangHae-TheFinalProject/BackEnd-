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
public class Reward {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long rewardId;

    @Column(nullable = false)
    private String rewardName;

    @Column(nullable = false)
    private Integer level;

    @Column
    private String rewardImg;

//    @JoinColumn(name="member_id")
//    @ManyToOne(fetch = FetchType.LAZY)
//    private Member member;

//    insert into reward (rewardName, level, rewardImg) values("뉴비 등장!", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");
//    insert into reward (rewardName, level, rewardImg) values("뉴비 등장!", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");
//    insert into reward (rewardName, level, rewardImg) values("뉴비의 걸음마뗴기", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");
//    insert into reward (rewardName, level, rewardImg) values("뉴비의 눈물나는 승리ㅠㅠ", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");
//    insert into reward (rewardName, level, rewardImg) values("하찮은 거짓말쟁이 5연승", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");
//    insert into reward (rewardName, level, rewardImg) values("선량한 시민 5연승", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");
//insert into reward (rewardName, level, rewardImg) values("선량한 시민", 1, "https://image.gamechosun.co.kr/wlwl_upload/dataroom/df/2021/12/03/583203_1638540976.jpg");

}
