package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.ProfileVaccineDiseaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileVaccineDiseaseStatusRepository extends JpaRepository<ProfileVaccineDiseaseStatus, Long> {
    List<ProfileVaccineDiseaseStatus> findByProfileId(Long profileId);
    Optional<ProfileVaccineDiseaseStatus> findByProfileIdAndDiseaseId(Long profileId, Long diseaseId);
}
