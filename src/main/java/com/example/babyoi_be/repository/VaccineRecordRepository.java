package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VaccineRecordRepository extends JpaRepository<VaccineRecord, Long> {
    List<VaccineRecord> findByProfileId(Long profileId);
    List<VaccineRecord> findByVaccineTypeId(Long vaccineTypeId);

    List<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status);
    List<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status, Sort sort);
    Page<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status, Pageable pageable);

    long countByProfileIdAndStatus(Long profileId, Long status);
    boolean existsByProfileIdAndVaccineTypeIdAndStatusIn(Long profileId, Long vaccineTypeId, Collection<Long> statuses);
    boolean existsByProfileIdAndVaccineTypeIdAndStatusInAndIdNot(Long profileId, Long vaccineTypeId, Collection<Long> statuses, Long id);
}
