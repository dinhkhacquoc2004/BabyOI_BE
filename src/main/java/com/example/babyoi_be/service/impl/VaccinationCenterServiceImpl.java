package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.respone.VaccinationCenterResponse;
import com.example.babyoi_be.domain.entity.VaccinePricing;
import com.example.babyoi_be.repository.VaccinePricingRepository;
import com.example.babyoi_be.service.VaccinationCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccinationCenterServiceImpl implements VaccinationCenterService {

    private final VaccinePricingRepository vaccinePricingRepository;

    @Override
    public List<VaccinationCenterResponse> getCentersByVaccineType(Long vaccineTypeId) {
        List<VaccinePricing> pricings = vaccinePricingRepository.findByVaccineTypeIdAndStatus(
                vaccineTypeId, Constants.TABLE_STATUS.ACTIVE);

        return pricings.stream().map(pricing -> VaccinationCenterResponse.builder()
                .id(pricing.getCenter().getId())
                .name(pricing.getCenter().getName())
                .address(pricing.getCenter().getAddress())
                .distance(pricing.getCenter().getDistance())
                .price(pricing.getPrice())
                .discountPrice(pricing.getDiscountPrice())
                .note(pricing.getNote())
                .build())
                .collect(Collectors.toList());
    }
}
