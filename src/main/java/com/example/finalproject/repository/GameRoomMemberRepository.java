package com.example.finalproject.repository;

import com.example.finalproject.domain.GameRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRoomMemberRepository extends JpaRepository<GameRoomMember, Long> {
}
