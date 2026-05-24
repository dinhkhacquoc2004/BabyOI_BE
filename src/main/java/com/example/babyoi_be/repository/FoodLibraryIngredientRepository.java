package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.FoodLibraryIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodLibraryIngredientRepository extends JpaRepository<FoodLibraryIngredient, Long> {
    @EntityGraph(attributePaths = {"foodIngredient", "foodIngredient.nutrition"})
    List<FoodLibraryIngredient> findByFoodLibraryId(Long foodId);
}
