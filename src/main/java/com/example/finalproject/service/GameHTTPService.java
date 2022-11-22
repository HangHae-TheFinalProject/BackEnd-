package com.example.finalproject.service;

import com.example.finalproject.controller.request.StringDto;
import com.example.finalproject.controller.response.VictoryDto;
import com.example.finalproject.controller.response.VoteDto;
import com.example.finalproject.domain.*;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.repository.GameStartSetRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.example.finalproject.domain.QGameRoomMember.gameRoomMember;

import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import static com.example.finalproject.domain.QMember.member;
@RequiredArgsConstructor
@Service
public class GameHTTPService {

    private final GameStartSetRepository gameStartSetRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final JPAQueryFactory jpaQueryFactory;
    private final TokenProvider tokenProvider;
    private final RewardRequired rewardRequired;
    static int cnt =0;
    static HashMap<String, Integer> voteHashMap = new HashMap<>();

//    public void isLier(Long gameroomid, String name) {
//        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
//
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(Long.toString(gameroomid));
//        gameMessage.setSenderId("");
//        gameMessage.setSender("");
//        gameMessage.setContent(gameStartSet.getLier().equals(name) +"");
//        gameMessage.setType(GameMessage.MessageType.START);
//        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);
//    }
    @Transactional
    public void vote(Long gameroomid, StringDto stringDto){
        String name = stringDto.getValue();
        voteHashMap.put(name, voteHashMap.getOrDefault(name, 0) + 1);
        cnt ++;

        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();
        int memberNum = gameRoomMembers.size();
        if(cnt != memberNum){
            VoteDto voteDto = new VoteDto(null);
            GameMessage<VoteDto> gameMessage = new GameMessage<>();
            gameMessage.setRoomId(Long.toString(gameroomid));
            gameMessage.setSenderId("");
            gameMessage.setSender("");
            gameMessage.setContent(voteDto);
            gameMessage.setType(GameMessage.MessageType.CONTINUE);
            messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

            return;
        }
        // 투표가 끝나면
//        List<String> nickName = sortHash();

        VoteDto voteDto = new VoteDto(sortHash());
        GameMessage<VoteDto> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");

        if(voteDto.getName().size()==1){
            if(voteDto.getName().get(0).equals(gameStartSet.getLier())){
//                content ="는 라이어가 맞습니다."; //LIER
                gameMessage.setType(GameMessage.MessageType.LIER);
            }
            else{
//                content = "는 라이어가 아닙니다."; //NLIER
                gameMessage.setType(GameMessage.MessageType.NLIER);
                gameStartSet.setWinner(GameStartSet.Winner.LIER);
            }
        }
        else{
            if (gameStartSet.getRound()<4){
//                content = "동점입니다."; //DRAW
                gameMessage.setType(GameMessage.MessageType.DRAW);
            }
            else{
//                content ="동점 입니다. 라이어의 승리 입니다. 라이어는 " +gameStartSet.getLier() + "입니다."; //DRAWANDENDGAME
                gameMessage.setType(GameMessage.MessageType.DRAWANDENDGAME);
                voteDto.setLierIs(gameStartSet.getLier());
                gameStartSet.setWinner(GameStartSet.Winner.LIER);
            }
        }
        gameMessage.setContent(voteDto);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

    }
    public List<String> sortHash(){
        List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(voteHashMap.entrySet());

        // 비교함수 Comparator를 사용하여 내림 차순으로 정렬
        Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
            // compare로 값을 비교
            public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2)
            {
                // 내림 차순으로 정렬
                return obj2.getValue().compareTo(obj1.getValue());
            }
        });
        int maxValue = 0;
        for(Entry<String, Integer> entry : list_entries) {
            maxValue = entry.getValue();
            break;
        }
        List<String> nickName = new ArrayList<>();
        for(Entry<String, Integer> entry : list_entries) {
            if(entry.getValue()==maxValue){
                nickName.add(entry.getKey());
            }
        }
        voteHashMap.clear();
        cnt=0;
        return nickName;
    }

    @Transactional
    public void isAnswer(Long gameroomid, StringDto stringDto) {
        String answer = stringDto.getValue();
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);

        GameMessage<Boolean> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        gameMessage.setContent(gameStartSet.getKeyword().equals(answer));
        gameMessage.setType(GameMessage.MessageType.RESULT);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

        if(gameStartSet.getKeyword().equals(answer)) {// 라이어가 정답을 맞추면
            gameStartSet.setWinner(GameStartSet.Winner.LIER);
        }
        else {
            gameStartSet.setWinner(GameStartSet.Winner.CITIZEN);
        }
    }

    @Transactional
    public void oneMoerRound(Long gameroomid) {
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        Integer round = gameStartSet.oneMoerRound();
        GameMessage<Integer> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        gameMessage.setContent(round);
        gameMessage.setType(GameMessage.MessageType.RESULT);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

    }
    @Transactional
    public void victory(Long gameroomid){
        GameStartSet gameStartSet= gameStartSetRepository.findByRoomId(gameroomid);
        System.out.println(gameStartSet.getGamestartsetId());

        List<GameRoomMember> gameRoomMembers = jpaQueryFactory
                .selectFrom(gameRoomMember)
                .where(gameRoomMember.gameroom_id.eq(gameroomid))
                .fetch();

        List<Member> playingMembers = new ArrayList<>();

        for(GameRoomMember gameRoomMember2 : gameRoomMembers){

            Member each_member = jpaQueryFactory
                    .selectFrom(member)
                    .where(member.memberId.eq(gameRoomMember2.getMember_id()))
                    .fetchOne();

            playingMembers.add(each_member);
        }
        VictoryDto victoryDto = new VictoryDto();
        if(gameStartSet.getWinner().equals(GameStartSet.Winner.LIER)){
            for(Member playingMember : playingMembers){
                if(playingMember.getNickname().equals(gameStartSet.getLier())){
                    playingMember.addWin();
                    victoryDto.getWinner().add(playingMember.getNickname());
                    playingMember.addWinLIER();
                }
                else {
                    playingMember.addLose();
                    victoryDto.getLoser().add(playingMember.getNickname());
                    playingMember.addLossCITIZEN();
                }

                // 게임 승리 업적
                rewardRequired.achieveVitoryReward(playingMember, gameroomid);
                // 게임 패배 업적
                rewardRequired.achieveLoseReward(playingMember, gameroomid);
            }
        }
        else{
            for(Member playingMember : playingMembers){
                if(playingMember.getNickname().equals(gameStartSet.getLier())){
                    playingMember.addLose();
                    victoryDto.getLoser().add(playingMember.getNickname());
                    playingMember.addLossLIER();
                }
                else {
                    playingMember.addWin();
                    victoryDto.getWinner().add(playingMember.getNickname());
                    playingMember.addWinCITIZEN();
                }

                // 게임 승리 업적
                rewardRequired.achieveVitoryReward(playingMember, gameroomid);
                //게임 패배 업적
                rewardRequired.achieveLoseReward(playingMember, gameroomid);
            }
        }
        GameMessage<VictoryDto> gameMessage = new GameMessage<>();
        gameMessage.setRoomId(Long.toString(gameroomid));
        gameMessage.setSenderId("");
        gameMessage.setSender("");
        gameMessage.setContent(victoryDto);
        gameMessage.setType(GameMessage.MessageType.RESULT);
        messagingTemplate.convertAndSend("/sub/gameroom/" + gameroomid, gameMessage);

    }
}