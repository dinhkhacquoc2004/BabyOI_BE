package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccinePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaccinePricingRepository extends JpaRepository<VaccinePricing, Long> {
    List<VaccinePricing> findByVaccineTypeIdAndStatus(Long vaccineTypeId, Long status);
}
