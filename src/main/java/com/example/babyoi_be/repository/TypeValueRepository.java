package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.TypeValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TypeValueRepository extends JpaRepository<TypeValue, Long> {
    @EntityGraph(attributePaths = "typeCode")
    List<TypeValue> findByTypeCodeCodeInAndStatusOrderByTypeCodeCodeAscSortOrderAscIdAsc(List<String> typeCodes, Long status);

    boolean existsByTypeCodeCodeIgnoreCaseAndValueCodeIgnoreCaseAndStatus(String typeCode, String valueCode, Long status);

    boolean existsByTypeCodeCodeIgnoreCaseAndValueTextIgnoreCaseAndStatus(String typeCode, String valueText, Long status);

    boolean existsByTypeCodeCodeIgnoreCaseAndValueNumberAndStatus(String typeCode, BigDecimal valueNumber, Long status);
}
