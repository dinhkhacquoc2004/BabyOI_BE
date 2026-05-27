package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.AiLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiLessonProgressRepository extends JpaRepository<AiLessonProgress, Long> {
    List<AiLessonProgress> findByProfileIdAndStatus(Long profileId, Long status);

    Optional<AiLessonProgress> findByProfileIdAndLessonIdAndStatus(Long profileId, Long lessonId, Long status);
}
