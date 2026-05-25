package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.VaccinationCenterResponse;
import java.util.List;

public interface VaccinationCenterService {
    List<VaccinationCenterResponse> getCentersByVaccineType(Long vaccineTypeId);
}
