package com.example.finalproject.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Media {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long mediaId;

    @Column(nullable = false)
    private String mediaName;

    @Column(nullable = false)
    private String mediaUrl;

    @JoinColumn(name = "postId")
    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

}
