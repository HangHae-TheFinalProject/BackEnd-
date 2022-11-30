package com.example.finalproject.controller.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class VictoryDto {
    List<String> winner = new ArrayList<>();
    List<String> loser = new ArrayList<>();
}
