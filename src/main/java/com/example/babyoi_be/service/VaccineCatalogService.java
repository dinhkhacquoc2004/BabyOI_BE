package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.*;

import java.util.List;

public interface VaccineCatalogService {
    List<LocationResponse> getLocations();
    List<VaccineResponse> getVaccines(String keyword);
    List<LocationVaccinePriceResponse> getVaccinesByLocation(Long locationId);
    List<VaccinePackageResponse> getPackages(String keyword);
    List<PackageStructureResponse> getPackageStructures(Long packageId, Integer durationMonths);
    List<LocationPackagePriceResponse> getPackagePricesByLocation(Long locationId, Long packageId);
}
