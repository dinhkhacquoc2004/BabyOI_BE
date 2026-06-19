package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.BabyRoutineEntryUpdateRequest;
import com.example.babyoi_be.domain.dto.request.BabyRoutineEntryCreateRequest;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineAiAnalysisResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineDayResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineEntryResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineHistoryResponse;
import com.example.babyoi_be.service.BabyRoutineService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/routines")
public class BabyRoutineController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BabyRoutineService babyRoutineService;

    public BabyRoutineController(BabyRoutineService babyRoutineService) {
        this.babyRoutineService = babyRoutineService;
    }

    @GetMapping("/profile/{profileId}")
    public BabyRoutineDayResponse getDailyRoutine(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return babyRoutineService.getDailyRoutine(profileId, date != null ? date : LocalDate.now(APP_ZONE));
    }

    @GetMapping("/profile/{profileId}/history")
    public BabyRoutineHistoryResponse getRoutineHistory(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate today = LocalDate.now(APP_ZONE);
        return babyRoutineService.getRoutineHistory(
                profileId,
                fromDate != null ? fromDate : today.withDayOfMonth(1),
                toDate != null ? toDate : today
        );
    }

    @PostMapping("/profile/{profileId}/next-day-adjustment")
    public BabyRoutineDayResponse applyNextDayRoutineAdjustment(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return babyRoutineService.applyNextDayRoutineAdjustment(profileId, date != null ? date : LocalDate.now(APP_ZONE));
    }

    @PostMapping("/profile/{profileId}/entries")
    public BabyRoutineEntryResponse createRoutineEntry(
            @PathVariable Long profileId,
            @Valid @RequestBody BabyRoutineEntryCreateRequest request
    ) {
        return babyRoutineService.createRoutineEntry(profileId, request);
    }

    @PatchMapping("/entries/{entryId}")
    public BabyRoutineEntryResponse updateRoutineEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody BabyRoutineEntryUpdateRequest request
    ) {
        return babyRoutineService.updateRoutineEntry(entryId, request);
    }

    @DeleteMapping("/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoutineEntry(@PathVariable Long entryId) {
        babyRoutineService.deleteRoutineEntry(entryId);
    }

    @PostMapping("/entries/{entryId}/timeline-update")
    public BabyRoutineDayResponse updateTimelineFromEntry(@PathVariable Long entryId) {
        return babyRoutineService.updateTimelineFromEntry(entryId);
    }

    @PostMapping("/profile/{profileId}/ai-analysis")
    public BabyRoutineAiAnalysisResponse analyzeRoutine(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return babyRoutineService.analyzeRoutine(profileId, date != null ? date : LocalDate.now(APP_ZONE));
    }
}
