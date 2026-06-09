package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.respone.*;
import com.example.babyoi_be.service.VaccineCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaccine-catalog")
@RequiredArgsConstructor
public class VaccineCatalogController {
    private final VaccineCatalogService vaccineCatalogService;

    @GetMapping("/locations")
    public List<LocationResponse> getLocations() {
        return vaccineCatalogService.getLocations();
    }

    @GetMapping("/vaccines")
    public List<VaccineResponse> getVaccines(@RequestParam(required = false) String keyword) {
        return vaccineCatalogService.getVaccines(keyword);
    }

    @GetMapping("/locations/{locationId}/vaccines")
    public List<LocationVaccinePriceResponse> getVaccinesByLocation(@PathVariable Long locationId) {
        return vaccineCatalogService.getVaccinesByLocation(locationId);
    }

    @GetMapping("/packages")
    public List<VaccinePackageResponse> getPackages(@RequestParam(required = false) String keyword) {
        return vaccineCatalogService.getPackages(keyword);
    }

    @GetMapping("/packages/{packageId}/structures")
    public List<PackageStructureResponse> getPackageStructures(
            @PathVariable Long packageId,
            @RequestParam(required = false) Integer durationMonths
    ) {
        return vaccineCatalogService.getPackageStructures(packageId, durationMonths);
    }

    @GetMapping("/locations/{locationId}/packages")
    public List<LocationPackagePriceResponse> getPackagePricesByLocation(
            @PathVariable Long locationId,
            @RequestParam(required = false) Long packageId
    ) {
        return vaccineCatalogService.getPackagePricesByLocation(locationId, packageId);
    }
}
