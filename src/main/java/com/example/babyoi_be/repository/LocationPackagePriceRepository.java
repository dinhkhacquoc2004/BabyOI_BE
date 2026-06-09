package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.LocationPackagePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationPackagePriceRepository extends JpaRepository<LocationPackagePrice, Long> {
    List<LocationPackagePrice> findByLocationId(Long locationId);
    List<LocationPackagePrice> findByLocationIdAndVaccinePackageId(Long locationId, Long packageId);
    Optional<LocationPackagePrice> findByLocationIdAndVaccinePackageIdAndDurationMonths(Long locationId, Long packageId, Integer durationMonths);
}
