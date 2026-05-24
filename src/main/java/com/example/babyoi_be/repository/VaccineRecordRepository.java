package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaccineRecordRepository extends JpaRepository<VaccineRecord, Long> {
    List<VaccineRecord> findByProfileId(Long profileId);
    List<VaccineRecord> findByVaccineTypeId(Long vaccineTypeId);
    
    List<VaccineRecord> findByProfileIdAndStatus(Long profileId, Long status);
    
    long countByProfileIdAndStatus(Long profileId, Long status);
}
