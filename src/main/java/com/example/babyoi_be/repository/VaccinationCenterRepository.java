package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccinationCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaccinationCenterRepository extends JpaRepository<VaccinationCenter, Long> {
    List<VaccinationCenter> findByStatus(Long status);
}
