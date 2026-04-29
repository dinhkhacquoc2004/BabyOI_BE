package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.service.VaccineRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vaccine-records")
@RequiredArgsConstructor
public class VaccineRecordController {

    private final VaccineRecordService vaccineRecordService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VaccineRecordResponse create(@Valid @RequestBody VaccineRecordRequest request) {
        return vaccineRecordService.createVaccineRecord(request);
    }

    @PutMapping("/{id}")
    public VaccineRecordResponse update(@PathVariable Long id, @Valid @RequestBody VaccineRecordRequest request) {
        return vaccineRecordService.updateVaccineRecord(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        vaccineRecordService.deleteVaccineRecord(id);
    }

    @GetMapping("/{id}")
    public VaccineRecordResponse getById(@PathVariable Long id) {
        return vaccineRecordService.getVaccineRecordById(id);
    }

    @GetMapping("/profile/{profileId}")
    public List<VaccineRecordResponse> getByStatus(
            @PathVariable Long profileId,
            @RequestParam Long status) {
        return vaccineRecordService.getVaccineRecordsByStatus(profileId, status);
    }

    @GetMapping("/profile/{profileId}/count")
    public long countByStatus(
            @PathVariable Long profileId,
            @RequestParam Long status) {
        return vaccineRecordService.countVaccineRecordsByStatus(profileId, status);
    }

    @GetMapping("/profile/{profileId}/statistics")
    public Map<String, Long> getStatistics(@PathVariable Long profileId) {
        return vaccineRecordService.getVaccineStatistics(profileId);
    }
}
