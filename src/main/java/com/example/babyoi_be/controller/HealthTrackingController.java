package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.HealthRecordRequest;
import com.example.babyoi_be.domain.dto.request.IllnessEventRequest;
import com.example.babyoi_be.domain.dto.respone.HealthMonthlyDetailResponse;
import com.example.babyoi_be.domain.dto.respone.HealthRecordResponse;
import com.example.babyoi_be.domain.dto.respone.IllnessEventResponse;
import com.example.babyoi_be.domain.dto.respone.TypeValueResponse;
import com.example.babyoi_be.service.HealthTrackingService;
import com.example.babyoi_be.service.TypeValueService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class HealthTrackingController {

    private final HealthTrackingService healthTrackingService;
    private final TypeValueService typeValueService;

    public HealthTrackingController(
            HealthTrackingService healthTrackingService,
            TypeValueService typeValueService
    ) {
        this.healthTrackingService = healthTrackingService;
        this.typeValueService = typeValueService;
    }

    @GetMapping("/api/health-records/filter-options")
    public Map<String, List<TypeValueResponse>> getHealthRecordFilterOptions() {
        return typeValueService.getValuesByCodes(List.of("ACTIVITY_LEVEL"));
    }

    @PostMapping("/api/health-records")
    @ResponseStatus(HttpStatus.CREATED)
    public HealthRecordResponse createHealthRecord(@Valid @RequestBody HealthRecordRequest request) {
        return healthTrackingService.createHealthRecord(request);
    }

    @PutMapping("/api/health-records/{id}")
    public HealthRecordResponse updateHealthRecord(@PathVariable Long id, @Valid @RequestBody HealthRecordRequest request) {
        return healthTrackingService.updateHealthRecord(id, request);
    }

    @DeleteMapping("/api/health-records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHealthRecord(@PathVariable Long id) {
        healthTrackingService.deleteHealthRecord(id);
    }

    @GetMapping("/api/health-records/{id}")
    public HealthRecordResponse getHealthRecord(@PathVariable Long id) {
        return healthTrackingService.getHealthRecord(id);
    }

    @GetMapping("/api/health-records/profile/{profileId}")
    public List<HealthRecordResponse> getHealthRecords(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return healthTrackingService.getHealthRecords(profileId, fromDate, toDate, month, year);
    }

    @GetMapping("/api/development-history/profile/{profileId}")
    public List<HealthRecordResponse> getDevelopmentHistory(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return healthTrackingService.getDevelopmentHistory(profileId, fromDate, toDate, month, year);
    }

    @GetMapping("/api/health-records/profile/{profileId}/monthly-detail")
    public HealthMonthlyDetailResponse getMonthlyDetail(
            @PathVariable Long profileId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return healthTrackingService.getMonthlyDetail(profileId, month, year);
    }

    @PostMapping("/api/illness-events")
    @ResponseStatus(HttpStatus.CREATED)
    public IllnessEventResponse createIllnessEvent(@Valid @RequestBody IllnessEventRequest request) {
        return healthTrackingService.createIllnessEvent(request);
    }

    @PutMapping("/api/illness-events/{id}")
    public IllnessEventResponse updateIllnessEvent(@PathVariable Long id, @Valid @RequestBody IllnessEventRequest request) {
        return healthTrackingService.updateIllnessEvent(id, request);
    }

    @DeleteMapping("/api/illness-events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIllnessEvent(@PathVariable Long id) {
        healthTrackingService.deleteIllnessEvent(id);
    }

    @GetMapping("/api/illness-events/{id}")
    public IllnessEventResponse getIllnessEvent(@PathVariable Long id) {
        return healthTrackingService.getIllnessEvent(id);
    }

    @GetMapping("/api/illness-events/profile/{profileId}")
    public List<IllnessEventResponse> getIllnessEvents(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long status
    ) {
        return healthTrackingService.getIllnessEvents(profileId, fromDate, toDate, month, year, status);
    }
}
