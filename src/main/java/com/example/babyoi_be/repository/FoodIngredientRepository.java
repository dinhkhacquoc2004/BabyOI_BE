package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.FoodIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodIngredientRepository extends JpaRepository<FoodIngredient, Long> {

    @EntityGraph(attributePaths = "nutrition")
    @Query("""
            select ingredient
            from FoodIngredient ingredient
            where ingredient.status is null or ingredient.status <> :deletedStatus
            """)
    List<FoodIngredient> findAvailableIngredients(@Param("deletedStatus") Long deletedStatus);
}
