package com.example.finalproject.repository;

import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMember(Member member);
}
