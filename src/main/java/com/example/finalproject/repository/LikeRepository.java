package com.example.finalproject.repository;

import com.example.finalproject.domain.Liked;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Liked, Long> {
}
