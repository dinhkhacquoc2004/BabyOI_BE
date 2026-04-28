package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccineTypeRepository extends JpaRepository<VaccineType, Long> {
    Optional<VaccineType> findByName(String name);
    List<VaccineType> findByForMotherTrue();
    List<VaccineType> findByForChildTrue();
    List<VaccineType> findByStatus(Long status);
}
