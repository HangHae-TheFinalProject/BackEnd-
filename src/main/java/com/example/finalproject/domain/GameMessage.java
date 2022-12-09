package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameMessage<T> {
    private String roomId;
    private String senderId;
    private String sender;
    private T content;
    private GameMessage.MessageType type;

    public enum MessageType {
        JOIN, READY, SPOTLIGHT, LEAVE, START, DRAW, ENDGAME, UPDATE, SWITCHING,
        CONTINUE, RESULT,DRAWANDENDGAME, LIER, NLIER, UNREADY, ALLREADY, LIAR, TRUER, COMPLETE, ALLCOMPLETE, WAIT, REWARD, VOTE, ONEMOREROUND,
        NEWOWNER, VICTORY, RESET
    }
}
