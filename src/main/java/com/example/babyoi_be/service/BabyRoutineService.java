package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.BabyRoutineEntryUpdateRequest;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineAiAnalysisResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineDayResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineEntryResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineHistoryResponse;

import java.time.LocalDate;

public interface BabyRoutineService {
    BabyRoutineDayResponse getDailyRoutine(Long profileId, LocalDate routineDate);

    BabyRoutineHistoryResponse getRoutineHistory(Long profileId, LocalDate fromDate, LocalDate toDate);

    BabyRoutineDayResponse applyNextDayRoutineAdjustment(Long profileId, LocalDate routineDate);

    BabyRoutineEntryResponse updateRoutineEntry(Long entryId, BabyRoutineEntryUpdateRequest request);

    BabyRoutineDayResponse updateTimelineFromEntry(Long entryId);

    BabyRoutineAiAnalysisResponse analyzeRoutine(Long profileId, LocalDate routineDate);
}
