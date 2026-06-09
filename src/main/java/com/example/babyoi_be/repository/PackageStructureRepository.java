package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.PackageStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageStructureRepository extends JpaRepository<PackageStructure, Long> {
    List<PackageStructure> findByVaccinePackageIdOrderByDurationMonthsAscRecommendedAgeMonthsAscDosageOrderAsc(Long packageId);
    List<PackageStructure> findByVaccinePackageIdAndDurationMonthsOrderByRecommendedAgeMonthsAscDosageOrderAsc(Long packageId, Integer durationMonths);
}
