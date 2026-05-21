package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.VaccineTypeResponse;
import java.util.List;

public interface VaccineTypeService {
    List<VaccineTypeResponse> getVaccineTypes(String keyword);
}
