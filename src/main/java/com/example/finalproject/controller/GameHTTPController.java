package com.example.finalproject.controller;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.service.GameHTTPService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("/lier/manager")
public class GameHTTPController {

    /*
        구현 해야 할 api
        - 투표하기
        - 라이어 정답 맞추기
        - 얘가 라이어가 맞나요 isLier
        - 한 바퀴 더
            0,1 할건지 말건지 -> 동점시 다 말할때
    */
    /*
        구현 해야 할 api
        - 한 바퀴 더
    */

    private final GameHTTPService gameHTTPService;

    @PostMapping("/{gameroomid}/isLier") // 프론트에서 라이어가 맞는지 확인할 때 사용. token필요 x
    @ResponseBody
    public Boolean isLier(@PathVariable Long gameroomid, @RequestBody StringDto stringDto) {
        return gameHTTPService.isLier(gameroomid, stringDto);
    }

    @PostMapping("/{gameroomid}/vote")
    @ResponseBody
    public ResponseEntity<PrivateResponseBody> vote(@PathVariable Long gameroomid, @RequestBody StringDto stringDto/*, HttpServletRequest request*/) {
        return gameHTTPService.vote(gameroomid, stringDto);
    }
    @PostMapping("/{gameroomid}/isAnswer") // 정답 맞는지 확인할 때 사용. token필요 x
    @ResponseBody
    public Boolean isAnswer(@PathVariable Long gameroomid, @RequestBody StringDto stringDto) {
        return gameHTTPService.isAnswer(gameroomid, stringDto);
    }

    @GetMapping("/{gameroomid}/oneMoerRound")
    @ResponseBody
    public ResponseEntity<PrivateResponseBody> oneMoerRound(@PathVariable Long gameroomid) {
        return gameHTTPService.oneMoerRound(gameroomid);
    }

    @GetMapping("/{gameroomid}/isRound") // 한바퀴 더 가능 여부 확인할 때 사용. token필요 x
    @ResponseBody
    public Boolean isRound(@PathVariable Long gameroomid) {
        return gameHTTPService.isRound(gameroomid);
    }


    /////////////////// test
    @PostMapping("/isLiertest") // 프론트에서 라이어가 맞는지 확인할 때 사용. token필요 x
    @ResponseBody
    public Boolean isLiertest(@RequestBody StringDto stringDto) {
        return gameHTTPService.isLiertest(stringDto);
    }

    @PostMapping("/{num}/votetest")
    @ResponseBody
    public ResponseEntity<PrivateResponseBody> votetest(@PathVariable int num, @RequestBody StringDto voteDto/*, HttpServletRequest request*/) {
        return gameHTTPService.votetest(num, voteDto);
    }

    @GetMapping("/oneMoerRoundtest")
    @ResponseBody
    public ResponseEntity<PrivateResponseBody> oneMoerRoundtest() {
        Integer test = 3;
        return new ResponseEntity<>(new PrivateResponseBody
                (StatusCode.OK, test), HttpStatus.OK);
    }

//    @PostMapping("/test")
//    @ResponseBody
//    public ResponseEntity<PrivateResponseBody> test(@RequestBody String name) {
//        System.out.println(name);
//        return new ResponseEntity<>(new PrivateResponseBody
//                (StatusCode.OK, null), HttpStatus.CONTINUE);
//    }
}
