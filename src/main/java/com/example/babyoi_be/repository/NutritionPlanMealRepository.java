package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.NutritionPlanMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionPlanMealRepository extends JpaRepository<NutritionPlanMeal, Long> {
}
