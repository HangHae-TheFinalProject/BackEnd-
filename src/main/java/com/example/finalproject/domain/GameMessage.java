package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GameMessage {

    private String roomId;
    private String senderId;
    private String sender;
    private String content;
    private MessageType type;

    public enum MessageType {
        JOIN, READY, SPOTLIGHT, LEAVE, START, PRECHECK, DRAW, ENDDRAW, SELECT, TURNCHECK, ENDGAME, ENDTURN, UPDATE, SWITCHING, WAIT, RESULT
    }
}