package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.*;

import java.util.List;

public interface VaccineCatalogService {
    List<VaccineResponse> getVaccines(String keyword);
    List<ChildVaccineDiseaseResponse> getChildVaccineDiseases();
    List<ChildDiseaseDoseVaccineOptionResponse> getChildDiseaseDoseVaccineOptions(Long diseaseId);
    ChildDiseasePlanResponse getChildDiseasePlan(Long diseaseId);
    List<VaccineProgressResponse> getVaccineProgress(Long profileId);
    VaccineProgressResponse stopProfileDiseaseSchedule(Long profileId, Long diseaseId);
    VaccineProgressResponse resumeProfileDiseaseSchedule(Long profileId, Long diseaseId);
}
