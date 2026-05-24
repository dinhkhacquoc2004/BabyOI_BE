package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.RestrictedFood;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestrictedFoodRepository extends JpaRepository<RestrictedFood, Long> {
    @EntityGraph(attributePaths = {"foodLibrary", "foodLibrary.nutritionSummary", "foodLibrary.recommendation"})
    List<RestrictedFood> findByProfileId(Long profileId);

    @EntityGraph(attributePaths = {"foodLibrary", "foodLibrary.nutritionSummary", "foodLibrary.recommendation"})
    Optional<RestrictedFood> findByProfileIdAndFoodLibraryId(Long profileId, Long foodId);
}
