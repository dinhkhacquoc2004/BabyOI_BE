package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import java.util.List;
import java.util.Map;

public interface VaccineRecordService {
    VaccineRecordResponse createVaccineRecord(VaccineRecordRequest request);
    VaccineRecordResponse updateVaccineRecord(Long id, VaccineRecordRequest request);
    void deleteVaccineRecord(Long id);
    VaccineRecordResponse getVaccineRecordById(Long id);
    List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status);
    List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status, Integer limit);
    PageResponse<VaccineRecordResponse> getVaccineRecordsPage(Long profileId, Long status, Integer page, Integer size);
    long countVaccineRecordsByStatus(Long profileId, Long status);
    Map<String, Long> getVaccineStatistics(Long profileId);
}
