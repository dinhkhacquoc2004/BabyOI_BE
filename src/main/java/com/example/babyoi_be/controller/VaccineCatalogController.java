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

    @GetMapping("/vaccines")
    public List<VaccineResponse> getVaccines(@RequestParam(required = false) String keyword) {
        return vaccineCatalogService.getVaccines(keyword);
    }

    @GetMapping("/child-diseases")
    public List<ChildVaccineDiseaseResponse> getChildVaccineDiseases() {
        return vaccineCatalogService.getChildVaccineDiseases();
    }

    @GetMapping("/child-diseases/{diseaseId}/dose-vaccine-options")
    public List<ChildDiseaseDoseVaccineOptionResponse> getChildDiseaseDoseVaccineOptions(@PathVariable Long diseaseId) {
        return vaccineCatalogService.getChildDiseaseDoseVaccineOptions(diseaseId);
    }

    @GetMapping("/child-diseases/{diseaseId}/plan")
    public ChildDiseasePlanResponse getChildDiseasePlan(@PathVariable Long diseaseId) {
        return vaccineCatalogService.getChildDiseasePlan(diseaseId);
    }

    @GetMapping("/profiles/{profileId}/progress")
    public List<VaccineProgressResponse> getVaccineProgress(@PathVariable Long profileId) {
        return vaccineCatalogService.getVaccineProgress(profileId);
    }

    @PatchMapping("/profiles/{profileId}/child-diseases/{diseaseId}/stop")
    public VaccineProgressResponse stopProfileDiseaseSchedule(
            @PathVariable Long profileId,
            @PathVariable Long diseaseId
    ) {
        return vaccineCatalogService.stopProfileDiseaseSchedule(profileId, diseaseId);
    }

    @PatchMapping("/profiles/{profileId}/child-diseases/{diseaseId}/resume")
    public VaccineProgressResponse resumeProfileDiseaseSchedule(
            @PathVariable Long profileId,
            @PathVariable Long diseaseId
    ) {
        return vaccineCatalogService.resumeProfileDiseaseSchedule(profileId, diseaseId);
    }
}
