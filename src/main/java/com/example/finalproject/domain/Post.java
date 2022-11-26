package com.example.finalproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Post extends Timestamped{

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long postId;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column
    private Long viewcnt;

    @JsonIgnore
    @JoinColumn(name = "memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @JsonIgnore
    @OneToMany(mappedBy = "post",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Media> medias;

    @OneToMany(mappedBy = "post",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

}
