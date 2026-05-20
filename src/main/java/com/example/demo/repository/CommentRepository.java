package com.example.demo.repository;

import com.example.demo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // Belirli bir etkinliğin yorumlarını tarihe göre sıralı getirir
    List<Comment> findByEvent_IdOrderByCreationDateDesc(Integer eventId);
}