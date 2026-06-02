package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.HandbookPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandbookPostRepository extends JpaRepository<HandbookPost, Long> {
    List<HandbookPost> findByStatusOrderByPublishedAtDescIdDesc(Long status);
    List<HandbookPost> findByStatusAndCategoryOrderByPublishedAtDescIdDesc(Long status, String category);
    List<HandbookPost> findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(Long status, String keyword);
    List<HandbookPost> findByStatusAndCategoryAndTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(Long status, String category, String keyword);
}
