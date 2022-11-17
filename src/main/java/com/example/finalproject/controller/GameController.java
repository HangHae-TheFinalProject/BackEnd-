package com.example.finalproject.controller;

import com.example.finalproject.domain.GameMessage;
import com.example.finalproject.service.GameService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@Controller
public class GameController {

    private final GameService gameService;

    // 게임 시작 (라이어 선언 / 키워드 공개)
    @GetMapping("/lier/game/{gameroomid}/start")
    public ResponseEntity<?> gameStart(
            HttpServletRequest request,
            @PathVariable Long gameroomid) {

        return gameService.gameStart(request, gameroomid);
    }


    @GetMapping("/lier/game/{gameroomid}/spotlight")
    public ResponseEntity<?> spotlight(
            @PathVariable Long gameroomid,
            HttpServletRequest request) {

        return gameService.spotlight(gameroomid, request);
    }

/////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////

    // /pub 사용 게임 시작 (라이어 선언 / 키워드 공개)
    @MessageMapping("/lier/game/{gameroomid}/start")
    public ResponseEntity<?> gameStartTest(
            GameMessage gameMessage,
            @DestinationVariable Long gameroomid) {

        return gameService.gameStartTest(gameMessage, gameroomid);
    }

    // pub 사용 스포트라이트
    @MessageMapping("/lier/game/{gameroomid}/spotlight")
    public ResponseEntity<?> spotlightTest(
            @DestinationVariable Long gameroomid
            ) {

        return gameService.spotlightTest(gameroomid);
    }


}
