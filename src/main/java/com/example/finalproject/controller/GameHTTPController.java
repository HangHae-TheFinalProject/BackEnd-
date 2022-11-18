package com.example.finalproject.controller;


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

    @MessageMapping("/lier/manager/{gameroomid}/vote/{name}")
    public void vote(@DestinationVariable("gameroomid") Long gameroomid, @DestinationVariable("name") String name) {
        gameHTTPService.vote(gameroomid, name);
    }
    @MessageMapping("/lier/manager/{gameroomid}/isAnswer/{answer}") // 정답 맞는지 확인할 때 사용. token필요 x
    public void isAnswer(@DestinationVariable("gameroomid") Long gameroomid, @DestinationVariable("answer") String answer) {
        gameHTTPService.isAnswer(gameroomid, answer);
    }

    @MessageMapping("/lier/manager/{gameroomid}/oneMoerRound")
    public void oneMoerRound(@DestinationVariable("gameroomid") Long gameroomid) {
        gameHTTPService.oneMoerRound(gameroomid);
    }

    @MessageMapping("/lier/manager/{gameroomid}/victory")
    public void victory(@DestinationVariable("gameroomid") Long gameroomid) {
        gameHTTPService.victory(gameroomid);
    }
}