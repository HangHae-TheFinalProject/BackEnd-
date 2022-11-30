package com.example.finalproject.controller;


import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.service.GameHTTPService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class GameHTTPController {

    private final GameHTTPService gameHTTPService;

//    @MessageMapping("/lier/manager/{gameroomid}/isLier/{name}")
//    public void isLier(@DestinationVariable("gameroomid") Long gameroomid,@DestinationVariable("name") String name) {
//        gameHTTPService.isLier(gameroomid, name);
//    }

    // 투표
    @MessageMapping("/lier/game/{gameroomid}/vote")
    public void vote(@DestinationVariable("gameroomid") Long gameroomid, StringDto stringDto) {
        gameHTTPService.vote(gameroomid, stringDto);
    }

    // 라이어 정답 맞추기
    @MessageMapping("/lier/game/{gameroomid}/isAnswer")
    public void isAnswer(@DestinationVariable("gameroomid") Long gameroomid, StringDto stringDto) {
        gameHTTPService.isAnswer(gameroomid, stringDto);
    }

//    // 한바퀴 더
//    @MessageMapping("/lier/manager/{gameroomid}/oneMoreRound")
//    public void oneMoerRound(@DestinationVariable("gameroomid") Long gameroomid) {
//        gameHTTPService.oneMoerRound(gameroomid);
//    }

    // winner, loser 전적 업데이트, 게임 종료
    @MessageMapping("/lier/game/{gameroomid}/endgame")
    public void endGame(@DestinationVariable("gameroomid") Long gameroomid) {
        gameHTTPService.endGame(gameroomid);
    }
}