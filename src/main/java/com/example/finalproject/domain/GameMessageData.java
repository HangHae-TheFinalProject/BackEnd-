package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameMessageData<T> {
    private String roomId;
    private String senderId;
    private String sender;
    private T data;
    private GameMessageData.MessageType type;

    public enum MessageType {
        JOIN, READY, SPOTLIGHT, LEAVE, START, PRECHECK, DRAW, ENDDRAW, SELECT, TURNCHECK, ENDGAME, ENDTURN, UPDATE, SWITCHING, CONTINUE, RESULT,DRAWANDENDGAME, LIER, NLIER
    }
}
