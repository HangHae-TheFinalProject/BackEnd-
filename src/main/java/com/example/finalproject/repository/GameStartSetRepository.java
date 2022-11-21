package com.example.finalproject.repository;

import com.example.finalproject.domain.GameStartSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStartSetRepository extends JpaRepository<GameStartSet, Long> {
    GameStartSet findByRoomId(Long roomId);
}
