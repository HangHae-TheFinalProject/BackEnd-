package com.example.finalproject.controller;


import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.service.GameRearService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class GameRearController {

    private final GameRearService gameRearService;

    // 투표
    @MessageMapping("/lier/game/{gameroomid}/vote")
    public void vote(@DestinationVariable("gameroomid") Long gameroomid, StringDto stringDto) {
        gameRearService.vote(gameroomid, stringDto);
    }

    // 라이어 정답 맞추기
    @MessageMapping("/lier/game/{gameroomid}/isAnswer")
    public void isAnswer(@DestinationVariable("gameroomid") Long gameroomid, StringDto stringDto) {
        gameRearService.isAnswer(gameroomid, stringDto);
    }

    // winner, loser 전적 업데이트, 게임 종료
    @MessageMapping("/lier/game/{gameroomid}/endgame")
    public void endGame(@DestinationVariable("gameroomid") Long gameroomid) {
        gameRearService.endGame(gameroomid);
    }
}