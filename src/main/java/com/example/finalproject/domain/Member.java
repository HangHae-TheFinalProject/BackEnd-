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

    @Column(nullable = false)
    private Long winNum;

    @Column(nullable = false)
    private Long winLIER;

    @Column(nullable = false)
    private Long winCITIZEN;

    @Column(nullable = false)
    private Long lossNum;

    @Column(nullable = false)
    private Long lossLIER;

    @Column(nullable = false)
    private Long lossCITIZEN;

    @JsonIgnore
    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    @JsonIgnore
    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @JsonIgnore
    @JoinColumn(name="gameroommember_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private GameRoomMember gameRoomMember;

    @JsonIgnore
    @OneToOne(mappedBy = "member",cascade = CascadeType.ALL, orphanRemoval = true)
    private MemberActive memberActive;

    @JsonIgnore
    @JoinColumn(name="memberreward_id")
    @OneToOne
    private MemberReward memberReward;

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

}
