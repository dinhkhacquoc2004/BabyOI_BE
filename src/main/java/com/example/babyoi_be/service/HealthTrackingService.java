package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.HealthRecordRequest;
import com.example.babyoi_be.domain.dto.request.IllnessEventRequest;
import com.example.babyoi_be.domain.dto.respone.HealthMonthlyDetailResponse;
import com.example.babyoi_be.domain.dto.respone.HealthRecordResponse;
import com.example.babyoi_be.domain.dto.respone.IllnessEventResponse;

import java.time.LocalDate;
import java.util.List;

public interface HealthTrackingService {
    HealthRecordResponse createHealthRecord(HealthRecordRequest request);

    HealthRecordResponse updateHealthRecord(Long id, HealthRecordRequest request);

    void deleteHealthRecord(Long id);

    HealthRecordResponse getHealthRecord(Long id);

    List<HealthRecordResponse> getHealthRecords(Long profileId, LocalDate fromDate, LocalDate toDate, Integer month, Integer year);

    List<HealthRecordResponse> getDevelopmentHistory(Long profileId, LocalDate fromDate, LocalDate toDate, Integer month, Integer year);

    HealthMonthlyDetailResponse getMonthlyDetail(Long profileId, Integer month, Integer year);

    IllnessEventResponse createIllnessEvent(IllnessEventRequest request);

    IllnessEventResponse updateIllnessEvent(Long id, IllnessEventRequest request);

    void deleteIllnessEvent(Long id);

    IllnessEventResponse getIllnessEvent(Long id);

    List<IllnessEventResponse> getIllnessEvents(Long profileId, LocalDate fromDate, LocalDate toDate, Integer month, Integer year, Long status);
}
