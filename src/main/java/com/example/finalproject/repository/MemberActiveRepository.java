package com.example.finalproject.repository;

import com.example.finalproject.domain.MemberActive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberActiveRepository extends JpaRepository<MemberActive, Long> {
}
