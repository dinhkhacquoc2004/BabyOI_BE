package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import java.util.Map;

public interface VaccineRecordService {
    VaccineRecordResponse createVaccineRecord(VaccineRecordRequest request);
    VaccineRecordResponse updateVaccineRecord(Long id, VaccineRecordRequest request);
    VaccineRecordResponse getVaccineRecordById(Long id);
    void deleteCustomVaccineRecord(Long id);
    Map<String, Long> getVaccineStatistics(Long profileId);
}
