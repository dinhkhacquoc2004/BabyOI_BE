package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.LocationVaccinePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationVaccinePriceRepository extends JpaRepository<LocationVaccinePrice, Long> {
    List<LocationVaccinePrice> findByLocationId(Long locationId);
    Optional<LocationVaccinePrice> findByLocationIdAndVaccineId(Long locationId, Long vaccineId);
}
