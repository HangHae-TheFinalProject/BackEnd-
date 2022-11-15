package com.example.finalproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Keyword {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long keywordId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String word;

}
