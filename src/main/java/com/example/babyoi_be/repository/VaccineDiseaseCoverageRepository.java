package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineDiseaseCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VaccineDiseaseCoverageRepository extends JpaRepository<VaccineDiseaseCoverage, Long> {
    List<VaccineDiseaseCoverage> findByStatus(Long status);
    List<VaccineDiseaseCoverage> findByDiseaseIdAndStatus(Long diseaseId, Long status);
    List<VaccineDiseaseCoverage> findByVaccineIdAndStatus(Long vaccineId, Long status);
    List<VaccineDiseaseCoverage> findByVaccineIdInAndStatus(Collection<Long> vaccineIds, Long status);
}
