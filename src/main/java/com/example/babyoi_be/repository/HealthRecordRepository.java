package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByProfileIdAndRecordDateBetweenOrderByRecordDateDesc(Long profileId, LocalDate fromDate, LocalDate toDate);

    List<HealthRecord> findByProfileIdAndRecordDateBetweenOrderByRecordDateAsc(Long profileId, LocalDate fromDate, LocalDate toDate);

    Optional<HealthRecord> findFirstByProfileIdAndRecordDateBetweenOrderByRecordDateDesc(Long profileId, LocalDate fromDate, LocalDate toDate);

    Optional<HealthRecord> findFirstByProfileIdOrderByRecordDateDesc(Long profileId);

    List<HealthRecord> findTop2ByProfileIdOrderByRecordDateDesc(Long profileId);
}
