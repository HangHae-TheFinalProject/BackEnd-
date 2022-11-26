package com.example.finalproject.repository;

import com.example.finalproject.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CommentRepository extends JpaRepository<Comment, Long> {
}
