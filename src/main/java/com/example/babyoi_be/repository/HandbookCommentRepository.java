package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.HandbookComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandbookCommentRepository extends JpaRepository<HandbookComment, Long> {
    List<HandbookComment> findByPostIdAndStatusOrderByCreatedAtAscIdAsc(Long postId, Long status);
    long countByPostIdAndStatus(Long postId, Long status);
}
