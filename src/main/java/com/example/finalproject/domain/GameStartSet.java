package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
public class GameStartSet {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long gamestartsetId;

    @Column
    private String lier;

    @Column
    private String category;

    @Column
    private String keyword;

    @Column
    private Long roomId;
}
