package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaccineScheduleRepository extends JpaRepository<VaccineSchedule, Long> {
    List<VaccineSchedule> findByVaccineTypeIdOrderByDoseNumberAsc(Long vaccineTypeId);
}
