package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.respone.VaccinationCenterResponse;
import com.example.babyoi_be.service.VaccinationCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaccination-centers")
@RequiredArgsConstructor
public class VaccinationCenterController {

    private final VaccinationCenterService vaccinationCenterService;

    @GetMapping("/vaccine-type/{vaccineTypeId}")
    public List<VaccinationCenterResponse> getByVaccineType(@PathVariable Long vaccineTypeId) {
        return vaccinationCenterService.getCentersByVaccineType(vaccineTypeId);
    }
}
