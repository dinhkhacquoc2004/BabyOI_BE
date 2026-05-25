package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.VaccineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccineTypeRepository extends JpaRepository<VaccineType, Long> {
    Optional<VaccineType> findByName(String name);
    
    @Query("SELECT v FROM VaccineType v WHERE " +
           "(:keyword IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND v.status = 1")
    List<VaccineType> searchByName(@Param("keyword") String keyword);

    List<VaccineType> findByForMotherTrue();
    List<VaccineType> findByForChildTrue();
    List<VaccineType> findByStatus(Long status);
}
