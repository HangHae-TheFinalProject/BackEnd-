package com.example.finalproject.controller;

import com.example.finalproject.domain.GameMessage;
import com.example.finalproject.domain.GameRoom;
import com.example.finalproject.domain.Member;
import com.example.finalproject.exception.PrivateException;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.jwt.TokenProvider;
import com.example.finalproject.service.GameService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.example.finalproject.domain.QGameRoom.gameRoom;


@RequiredArgsConstructor
@Controller
public class GameController {

    private final GameService gameService;
    private final JPAQueryFactory jpaQueryFactory;
    private final SimpMessageSendingOperations messagingTemplate;


    // 게임 시작 (라이어 선언 / 키워드 공개)
    @GetMapping("/lier/game/{gameroomid}/start")
    public ResponseEntity<?> gameStart(
            HttpServletRequest request,
            @PathVariable Long gameroomid) {

        return gameService.gameStart(request, gameroomid);
    }


//    @Scheduled(fixedDelay = 15000)
    @GetMapping("/lier/game/{gameroomid}/spotlight")
    public ResponseEntity<?> spotlight(
            @PathVariable Long gameroomid,
            HttpServletRequest request) {


        return gameService.spotlight(gameroomid, request);
    }


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // stomp


//    @MessageMapping("/game/{roomId}")
//    public void gameMessageProxy(@Payload GameMessage message) throws JsonProcessingException {
//        System.out.println("여기에 들어오나 메시지매핑 메서드");
//        if (GameMessage.MessageType.START.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            gameStarter(message);
//        }
//        if (GameMessage.MessageType.PRECHECK.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            precheck(message);
//        }
//        if (GameMessage.MessageType.DRAW.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            draw(message);
//        }
//        if (GameMessage.MessageType.SELECT.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            select(message);
//        }
//        if (GameMessage.MessageType.TURNCHECK.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            turnCheck(message);
//        }
//        if (GameMessage.MessageType.USECARD.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            useCard(message);
//        }
//        if (GameMessage.MessageType.USESPECIAL.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            useSpecial(message);
//        }
//        if (GameMessage.MessageType.DISCARD.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            discard(message);
//        }
//        if (GameMessage.MessageType.ENDTURN.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            endCheck(message);
//        }
//        if (GameMessage.MessageType.ENDGAME.equals(message.getType())) {
//            System.out.println("여기에 들어오나" + message.getType());
//            endGame(message);
//        }
//    }


//    public void gameStarter(GameMessage message) throws JsonProcessingException {
//        System.out.println("여기에 들어오나 게임스타터");
//        gameStarter.createGameRoom(message.getRoomId());
//        Game game = gameRepository.findByRoomId(message.getRoomId());
//        String messageContent = jsonStringBuilder.gameStarter(game);
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setType(GameMessage.MessageType.START);
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }
//
//    private void precheck(GameMessage message) throws JsonProcessingException {
//        String messageContent = preTurn.preturnStartCheck(message.getSender());
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setType(GameMessage.MessageType.PRECHECK);
//        gameMessage.setSender(message.getSender());
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }

//    private void draw(GameMessage message)  throws JsonProcessingException {
//        String messageContent = preTurn.cardDrawInitiator(message.getSender());
//        GameMessage gameMessage = new GameMessage();
//        if (messageContent.contains("endDraw")) {
//            gameMessage.setType(GameMessage.MessageType.ENDDRAW);
//        } else {gameMessage.setType(GameMessage.MessageType.DRAW);}
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }
//
//    private void select(GameMessage message) throws JsonProcessingException {
//        System.out.println("여기에 들어오나 셀렉트 메서드");
//        System.out.println(message.getContent() + " 이상 콘텐츠로 들어오는 스트링 내용");
//        CardSelectRequestDto requestDto = objectBuilder.drawnCards(message.getContent());
//        String messageContent = preTurn.cardDrawResponse(message.getSender(), requestDto);
//        GameMessage gameMessage = new GameMessage();
//        if (messageContent.contains("endDraw")) {
//            gameMessage.setType(GameMessage.MessageType.ENDDRAW);
//        } else {gameMessage.setType(GameMessage.MessageType.SELECT);}
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setContent(messageContent);
//        System.out.println(messageContent);
//        System.out.println(message.getRoomId());
//        System.out.println(message.getSender());
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }
//
//    private void turnCheck(GameMessage message) throws JsonProcessingException {
//        String messageContent = preTurn.actionTurnCheck(message.getSender());
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setType(GameMessage.MessageType.TURNCHECK);
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }
//
//    private void useCard(GameMessage message) throws JsonProcessingException {
//        UseCardDto dto = objectBuilder.cardUse(message.getContent());
//        String messageContent = actionTurn.cardMoveProcess(message.getSender(), dto);
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        if (messageContent.equals("마나부족") || messageContent.equals("침묵됨")) {
//            String newMessageContent = jsonStringBuilder.cardUseFailDtoJsonBuilder(false);
//            gameMessage.setContent(newMessageContent);
//            gameMessage.setType(GameMessage.MessageType.USEFAIL);
//            messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//        } else {
//            gameMessage.setContent(messageContent);
//            gameMessage.setType(GameMessage.MessageType.USECARD);
//            messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//        }
//    }
//
//    private void useSpecial(GameMessage message) {
//    }
//
//    private void discard(GameMessage message) throws JsonProcessingException {
//        DiscardDto dto = objectBuilder.discard(message.getContent());
//        String messageContent = actionTurn.discard(message.getSender(), dto);
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setType(GameMessage.MessageType.DISCARD);
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }
//
//    private void endCheck(GameMessage message) throws JsonProcessingException {
//        String messageContent = endTurn.EndTurnCheck(message.getSender());
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setType(GameMessage.MessageType.ENDTURN);
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }
//
//    private void endGame(GameMessage message) throws JsonProcessingException {
//        String messageContent = endGame.gameEnd(message.getRoomId());
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(message.getRoomId());
//        gameMessage.setSender(message.getSender());
//        gameMessage.setType(GameMessage.MessageType.ENDGAME);
//        gameMessage.setContent(messageContent);
//        messagingTemplate.convertAndSend("/sub/game/" + message.getRoomId(), gameMessage);
//    }

    private void update(GameMessage message) throws JsonProcessingException {
        String roomId = message.getRoomId();
//        GameRoom room = gameRoomRepository.findByRoomId(roomId);
        GameRoom existGameRoom = jpaQueryFactory
                .selectFrom(gameRoom)
                .where(gameRoom.roomId.eq(Long.parseLong(roomId)))
                .fetchOne();

        if (existGameRoom != null) {
//            String userListMessage = jsonStringBuilder.gameRoomResponseDtoJsonBuilder(room);
            GameMessage gameMessage = new GameMessage();
            gameMessage.setRoomId(roomId);
            gameMessage.setSender(message.getSender());
//            gameMessage.setContent(userListMessage);
            gameMessage.setType(GameMessage.MessageType.UPDATE);
            messagingTemplate.convertAndSend("/sub/groom/" + roomId, gameMessage);
        }
    }

//    private void ready(GameMessage message) throws JsonProcessingException {
//        String roomId = message.getRoomId();
//        GameRoom room = gameRoomRepository.findByRoomId(roomId);
//        if (room != null) {
//            if (room.getPlayer1() != null) {
//                if (room.getPlayer1().equals(message.getSender()) ||
//                        room.getPlayer1().equals(message.getSender() * -1)) {
//                    room.setPlayer1(room.getPlayer1() * -1);
//                }
//            }
//            if (room.getPlayer2() != null) {
//                if (room.getPlayer2().equals(message.getSender()) ||
//                        room.getPlayer2().equals(message.getSender() * -1)) {
//                    room.setPlayer2(room.getPlayer2() * -1);
//                }
//            }
//            if (room.getPlayer3() != null) {
//                if (room.getPlayer3().equals(message.getSender()) ||
//                        room.getPlayer3().equals(message.getSender() * -1)) {
//                    room.setPlayer3(room.getPlayer3() * -1);
//                }
//            }
//            if (room.getPlayer4() != null) {
//                if (room.getPlayer4().equals(message.getSender()) ||
//                        room.getPlayer4().equals(message.getSender() * -1)) {
//                    room.setPlayer4(room.getPlayer4() * -1);
//                }
//            }
//            gameRoomRepository.save(room);
//            String userListMessage = jsonStringBuilder.gameRoomResponseDtoJsonBuilder(room);
//            GameMessage gameMessage = new GameMessage();
//            gameMessage.setRoomId(roomId);
//            gameMessage.setSender(message.getSender());
//            gameMessage.setContent(userListMessage);
//            gameMessage.setType(GameMessage.MessageType.UPDATE);
//            messagingTemplate.convertAndSend("/sub/groom/" + roomId, gameMessage);
//        }
//    }
//
//    private void switchingPosition(GameMessage message) throws JsonProcessingException {
//        System.out.println("여기에 들어오나 스위칭 포지션");
//        String roomId = message.getRoomId();
//        GameRoom room = gameRoomRepository.findByRoomId(roomId);
//        SwitchingPositionRequestDto requestDto = objectBuilder.switching(message.getContent());
//        GameMessage gameMessage = new GameMessage();
//        gameMessage.setRoomId(roomId);
//        gameMessage.setSender(message.getSender());
//        gameMessage.setContent(gameRoomService.switchingPosition(room, requestDto));
//        gameMessage.setType(GameMessage.MessageType.UPDATE);
//        messagingTemplate.convertAndSend("/sub/groom/" + roomId, gameMessage);
//    }

}
