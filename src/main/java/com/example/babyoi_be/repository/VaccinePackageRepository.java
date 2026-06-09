package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccinePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccinePackageRepository extends JpaRepository<VaccinePackage, Long> {
    Optional<VaccinePackage> findByCode(String code);
    List<VaccinePackage> findByStatus(Long status);
}
