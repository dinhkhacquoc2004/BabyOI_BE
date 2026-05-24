package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.FavoriteFood;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteFoodRepository extends JpaRepository<FavoriteFood, Long> {
    @EntityGraph(attributePaths = {"foodLibrary", "foodLibrary.nutritionSummary", "foodLibrary.recommendation"})
    List<FavoriteFood> findByProfileId(Long profileId);

    @EntityGraph(attributePaths = {"foodLibrary", "foodLibrary.nutritionSummary", "foodLibrary.recommendation"})
    Optional<FavoriteFood> findByProfileIdAndFoodLibraryId(Long profileId, Long foodId);
}
