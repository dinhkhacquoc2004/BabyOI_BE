package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.NutritionPlanDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionPlanDayRepository extends JpaRepository<NutritionPlanDay, Long> {
}
