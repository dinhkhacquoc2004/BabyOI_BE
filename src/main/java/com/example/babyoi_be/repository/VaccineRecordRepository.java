package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface VaccineRecordRepository extends JpaRepository<VaccineRecord, Long> {
    List<VaccineRecord> findByProfileId(Long profileId);
    List<VaccineRecord> findByVaccineId(Long vaccineId);
    List<VaccineRecord> findByProfileIdAndSource(Long profileId, Long source);

    List<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status);
    List<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status, Sort sort);
    Page<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status, Pageable pageable);

    long countByProfileIdAndStatus(Long profileId, Long status);
    boolean existsByProfileIdAndVaccineIdAndStatusIn(Long profileId, Long vaccineId, Collection<Long> statuses);
    boolean existsByProfileIdAndVaccineIdAndStatusInAndIdNot(Long profileId, Long vaccineId, Collection<Long> statuses, Long id);
    boolean existsByProfileIdAndVaccineIdAndInjectionDateAndStatusIn(Long profileId, Long vaccineId, LocalDate injectionDate, Collection<Long> statuses);
    boolean existsByProfileIdAndVaccineIdAndInjectionDateAndStatusInAndIdNot(Long profileId, Long vaccineId, LocalDate injectionDate, Collection<Long> statuses, Long id);

    List<VaccineRecord> findByInjectionDateAndStatus(LocalDate injectionDate, Long status);
}
