package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.ChildDiseaseDoseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildDiseaseDoseScheduleRepository extends JpaRepository<ChildDiseaseDoseSchedule, Long> {
    List<ChildDiseaseDoseSchedule> findByDiseaseIdAndStatusOrderByDoseOrderAsc(Long diseaseId, Long status);
    List<ChildDiseaseDoseSchedule> findByStatusOrderByDiseaseDisplayOrderAscDoseOrderAsc(Long status);
}
