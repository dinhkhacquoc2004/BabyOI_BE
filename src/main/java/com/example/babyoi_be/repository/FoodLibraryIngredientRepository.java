package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.FoodLibraryIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodLibraryIngredientRepository extends JpaRepository<FoodLibraryIngredient, Long> {
    @EntityGraph(attributePaths = {"foodIngredient", "foodIngredient.nutrition"})
    List<FoodLibraryIngredient> findByFoodLibraryId(Long foodId);

    @Query("""
            select fli
            from FoodLibraryIngredient fli
            join fetch fli.foodLibrary food
            left join fetch food.nutritionSummary
            left join fetch food.recommendation
            join fetch fli.foodIngredient ingredient
            where food.status = :foodStatus
              and (fli.status is null or fli.status <> :deletedStatus)
              and (ingredient.status is null or ingredient.status <> :deletedStatus)
            """)
    List<FoodLibraryIngredient> findActiveRecipeIngredients(
            @Param("foodStatus") Long foodStatus,
            @Param("deletedStatus") Long deletedStatus);
}
