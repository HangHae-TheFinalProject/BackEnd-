package com.example.finalproject.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Member extends Timestamped{

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long memberId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column
    private Long winNum = 0L;

    @Column
    private Long winLIER = 0L;

    @Column
    private Long winCITIZEN = 0L;

    @Column
    private Long lossNum = 0L;

    @Column
    private Long lossLIER = 0L;

    @Column
    private Long lossCITIZEN = 0L;

    @JsonIgnore
    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    @JsonIgnore
    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    // 추가
    @JsonIgnore
    @JoinColumn(name="gameroommember_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private GameRoomMember gameRoomMember;

    @JsonIgnore
    @OneToMany(fetch = FetchType.EAGER)
    private List<Reward> rewards;

    @JsonIgnore
    @OneToOne(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private MemberActive memberActive;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Member member = (Member) o;
        return memberId != null && Objects.equals(memberId, member.memberId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean validatePassword(PasswordEncoder passwordEncoder, String password) {
        return passwordEncoder.matches(password, this.password);
    }

    public void update(List<Reward> rewards){
        this.rewards = rewards;
    }
}
