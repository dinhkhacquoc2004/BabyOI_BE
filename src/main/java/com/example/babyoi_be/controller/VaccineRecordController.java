package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.service.VaccineRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public VaccineRecordResponse getById(@PathVariable Long id) {
        return vaccineRecordService.getVaccineRecordById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        vaccineRecordService.deleteCustomVaccineRecord(id);
    }

    @GetMapping("/profile/{profileId}/statistics")
    public Map<String, Long> getStatistics(@PathVariable Long profileId) {
        return vaccineRecordService.getVaccineStatistics(profileId);
    }
}
