package com.example.finalproject.controller.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VoteDto {
    List<String> name;
    String lierIs;

    public VoteDto(List<String> name){
        this.name=name;
        lierIs="";
    }

}
