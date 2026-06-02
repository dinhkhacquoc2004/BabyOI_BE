package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.RestrictedFood;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestrictedFoodRepository extends JpaRepository<RestrictedFood, Long> {
    @EntityGraph(attributePaths = {"foodLibrary", "foodLibrary.nutritionSummary"})
    List<RestrictedFood> findByProfileId(Long profileId);

    @EntityGraph(attributePaths = {"foodLibrary", "foodLibrary.nutritionSummary"})
    Optional<RestrictedFood> findByProfileIdAndFoodLibraryId(Long profileId, Long foodId);

    @Query("select rf.foodLibrary.id from RestrictedFood rf where rf.profile.id = :profileId")
    List<Long> findFoodLibraryIdsByProfileId(@Param("profileId") Long profileId);
}
