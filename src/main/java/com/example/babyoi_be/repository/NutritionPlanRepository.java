package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.NutritionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, Long> {
    Optional<NutritionPlan> findFirstByRequestHashAndStatusOrderByCreatedAtDesc(String requestHash, Long status);
}
