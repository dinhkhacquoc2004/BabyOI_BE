package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.ChildVaccineDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildVaccineDiseaseRepository extends JpaRepository<ChildVaccineDisease, Long> {
    List<ChildVaccineDisease> findByStatusOrderByDisplayOrderAscIdAsc(Long status);
    Optional<ChildVaccineDisease> findByCode(String code);
}
