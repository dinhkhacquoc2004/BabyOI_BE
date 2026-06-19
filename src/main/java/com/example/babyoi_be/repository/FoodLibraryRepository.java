package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.FoodLibrary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodLibraryRepository extends JpaRepository<FoodLibrary, Long> {
    @EntityGraph(attributePaths = "nutritionSummary")
    List<FoodLibrary> findByStatus(Long status);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatus(Long status, Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndFunctionCode(Long status, Long functionCode, Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndNameContainingIgnoreCase(Long status, String name, Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndAdvanceFor(Long status, String advanceFor, Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndAdvanceForIn(Long status, List<String> advanceFor, Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndFunctionCodeAndNameContainingIgnoreCase(
            Long status,
            Long functionCode,
            String name,
            Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndFunctionCodeAndAdvanceFor(
            Long status,
            Long functionCode,
            String advanceFor,
            Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndFunctionCodeAndAdvanceForIn(
            Long status,
            Long functionCode,
            List<String> advanceFor,
            Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndAdvanceForAndNameContainingIgnoreCase(
            Long status,
            String advanceFor,
            String name,
            Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndAdvanceForInAndNameContainingIgnoreCase(
            Long status,
            List<String> advanceFor,
            String name,
            Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndFunctionCodeAndAdvanceForAndNameContainingIgnoreCase(
            Long status,
            Long functionCode,
            String advanceFor,
            String name,
            Pageable pageable);

    @EntityGraph(attributePaths = "nutritionSummary")
    Page<FoodLibrary> findByStatusAndFunctionCodeAndAdvanceForInAndNameContainingIgnoreCase(
            Long status,
            Long functionCode,
            List<String> advanceFor,
            String name,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"nutritionSummary", "recommendation"})
    Optional<FoodLibrary> findById(Long id);

    @EntityGraph(attributePaths = {"nutritionSummary", "recommendation"})
    List<FoodLibrary> findTop50ByStatusOrderByIdAsc(Long status);

    @EntityGraph(attributePaths = {"nutritionSummary", "recommendation"})
    List<FoodLibrary> findByIdInAndStatus(List<Long> ids, Long status);
}
