package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.ChildDiseaseDoseVaccineOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildDiseaseDoseVaccineOptionRepository extends JpaRepository<ChildDiseaseDoseVaccineOption, Long> {
    List<ChildDiseaseDoseVaccineOption> findByDiseaseIdAndStatusOrderByDoseOrderAscDisplayOrderAscIdAsc(Long diseaseId, Long status);
}
