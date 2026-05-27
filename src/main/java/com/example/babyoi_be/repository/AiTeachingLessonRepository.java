package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.AiTeachingLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiTeachingLessonRepository extends JpaRepository<AiTeachingLesson, Long> {
    List<AiTeachingLesson> findByStatusOrderBySortOrderAscIdAsc(Long status);
}
