package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.DiaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    List<DiaryEntry> findByProfileIdAndStatusAndEntryDateBetweenOrderByEntryDateDescCreatedAtDescIdDesc(
            Long profileId,
            Long status,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            SELECT entry FROM DiaryEntry entry
            WHERE entry.profile.id = :profileId
              AND entry.status = :status
              AND entry.entryDate BETWEEN :fromDate AND :toDate
              AND (
                  LOWER(entry.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(entry.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(entry.milestone) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY entry.entryDate DESC, entry.createdAt DESC, entry.id DESC
            """)
    List<DiaryEntry> searchActiveEntries(
            @Param("profileId") Long profileId,
            @Param("status") Long status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("keyword") String keyword
    );

    long countByProfileIdAndStatus(Long profileId, Long status);
}
