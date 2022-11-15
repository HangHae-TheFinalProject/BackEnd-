package com.example.finalproject.controller;

import com.example.finalproject.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RequestMapping("/lier/game")
@Controller
public class GameController {

    private final GameService gameService;

    // 게임 시작 (라이어 선언 / 키워드 공개)
    @GetMapping("/start/{gameroomid}")
    public ResponseEntity<?> gameStart(
            HttpServletRequest request,
            @PathVariable Long gameroomid){

        return gameService.gameStart(request, gameroomid);
    }

    @GetMapping("/{gameroomid}/spotlight/{spotNum}")
    public ResponseEntity<?> spotlight(
            @PathVariable Long gameroomid,
            @PathVariable int spotNum,
            HttpServletRequest request){

        return gameService.spotlight(gameroomid,spotNum, request);
    }
}
