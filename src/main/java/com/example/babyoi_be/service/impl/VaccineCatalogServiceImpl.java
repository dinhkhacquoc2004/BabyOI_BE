package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.utils.TextSearchUtils;
import com.example.babyoi_be.domain.dto.respone.*;
import com.example.babyoi_be.domain.entity.*;
import com.example.babyoi_be.repository.*;
import com.example.babyoi_be.service.VaccineCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccineCatalogServiceImpl implements VaccineCatalogService {
    private final LocationRepository locationRepository;
    private final VaccineRepository vaccineRepository;
    private final LocationVaccinePriceRepository locationVaccinePriceRepository;
    private final VaccinePackageRepository vaccinePackageRepository;
    private final PackageStructureRepository packageStructureRepository;
    private final LocationPackagePriceRepository locationPackagePriceRepository;

    @Override
    public List<LocationResponse> getLocations() {
        return locationRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapLocation)
                .collect(Collectors.toList());
    }

    @Override
    public List<VaccineResponse> getVaccines(String keyword) {
        return vaccineRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(vaccine -> keyword == null || keyword.trim().isEmpty() || TextSearchUtils.contains(vaccine.getName(), keyword))
                .map(this::mapVaccine)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationVaccinePriceResponse> getVaccinesByLocation(Long locationId) {
        return locationVaccinePriceRepository.findByLocationId(locationId)
                .stream()
                .map(this::mapLocationVaccinePrice)
                .collect(Collectors.toList());
    }

    @Override
    public List<VaccinePackageResponse> getPackages(String keyword) {
        return vaccinePackageRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(pkg -> keyword == null || keyword.trim().isEmpty() || TextSearchUtils.contains(pkg.getName(), keyword) || TextSearchUtils.contains(pkg.getCode(), keyword))
                .map(this::mapPackage)
                .collect(Collectors.toList());
    }

    @Override
    public List<PackageStructureResponse> getPackageStructures(Long packageId, Integer durationMonths) {
        List<PackageStructure> structures = durationMonths == null
                ? packageStructureRepository.findByVaccinePackageIdOrderByDurationMonthsAscRecommendedAgeMonthsAscDosageOrderAsc(packageId)
                : packageStructureRepository.findByVaccinePackageIdAndDurationMonthsOrderByRecommendedAgeMonthsAscDosageOrderAsc(packageId, durationMonths);

        return structures.stream().map(this::mapPackageStructure).collect(Collectors.toList());
    }

    @Override
    public List<LocationPackagePriceResponse> getPackagePricesByLocation(Long locationId, Long packageId) {
        List<LocationPackagePrice> prices = packageId == null
                ? locationPackagePriceRepository.findByLocationId(locationId)
                : locationPackagePriceRepository.findByLocationIdAndVaccinePackageId(locationId, packageId);

        return prices.stream().map(this::mapLocationPackagePrice).collect(Collectors.toList());
    }

    private LocationResponse mapLocation(Location location) {
        return LocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .region(location.getRegion())
                .address(location.getAddress())
                .status(location.getStatus())
                .build();
    }

    private VaccineResponse mapVaccine(Vaccine vaccine) {
        return VaccineResponse.builder()
                .id(vaccine.getId())
                .name(vaccine.getName())
                .manufacturer(vaccine.getManufacturer())
                .origin(vaccine.getOrigin())
                .description(vaccine.getDescription())
                .status(vaccine.getStatus())
                .build();
    }

    private LocationVaccinePriceResponse mapLocationVaccinePrice(LocationVaccinePrice price) {
        Location location = price.getLocation();
        Vaccine vaccine = price.getVaccine();
        return LocationVaccinePriceResponse.builder()
                .id(price.getId())
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getName() : null)
                .vaccineId(vaccine != null ? vaccine.getId() : null)
                .vaccineName(vaccine != null ? vaccine.getName() : null)
                .manufacturer(vaccine != null ? vaccine.getManufacturer() : null)
                .origin(vaccine != null ? vaccine.getOrigin() : null)
                .retailPrice(price.getRetailPrice())
                .stockStatus(price.getStockStatus())
                .status(price.getStatus())
                .build();
    }

    private VaccinePackageResponse mapPackage(VaccinePackage vaccinePackage) {
        return VaccinePackageResponse.builder()
                .id(vaccinePackage.getId())
                .code(vaccinePackage.getCode())
                .name(vaccinePackage.getName())
                .description(vaccinePackage.getDescription())
                .status(vaccinePackage.getStatus())
                .build();
    }

    private PackageStructureResponse mapPackageStructure(PackageStructure structure) {
        VaccinePackage vaccinePackage = structure.getVaccinePackage();
        Vaccine vaccine = structure.getVaccine();
        return PackageStructureResponse.builder()
                .id(structure.getId())
                .packageId(vaccinePackage != null ? vaccinePackage.getId() : null)
                .packageName(vaccinePackage != null ? vaccinePackage.getName() : null)
                .durationMonths(structure.getDurationMonths())
                .vaccineId(vaccine != null ? vaccine.getId() : null)
                .vaccineName(vaccine != null ? vaccine.getName() : null)
                .recommendedAgeMonths(structure.getRecommendedAgeMonths())
                .dosageOrder(structure.getDosageOrder())
                .doseLabel(structure.getDoseLabel())
                .note(structure.getNote())
                .build();
    }

    private LocationPackagePriceResponse mapLocationPackagePrice(LocationPackagePrice price) {
        Location location = price.getLocation();
        VaccinePackage vaccinePackage = price.getVaccinePackage();
        return LocationPackagePriceResponse.builder()
                .id(price.getId())
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getName() : null)
                .packageId(vaccinePackage != null ? vaccinePackage.getId() : null)
                .packageCode(vaccinePackage != null ? vaccinePackage.getCode() : null)
                .packageName(vaccinePackage != null ? vaccinePackage.getName() : null)
                .durationMonths(price.getDurationMonths())
                .baseVaccineSum(price.getBaseVaccineSum())
                .serviceFee(price.getServiceFee())
                .discountAmount(price.getDiscountAmount())
                .finalPackagePrice(price.getFinalPackagePrice())
                .giftDescription(price.getGiftDescription())
                .build();
    }
}
