package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.DiaryEntryRequest;
import com.example.babyoi_be.domain.dto.respone.DiaryEntryResponse;

import java.time.LocalDate;
import java.util.List;

public interface DiaryEntryService {
    DiaryEntryResponse createDiaryEntry(DiaryEntryRequest request);

    DiaryEntryResponse updateDiaryEntry(Long id, DiaryEntryRequest request);

    void deleteDiaryEntry(Long id);

    DiaryEntryResponse getDiaryEntry(Long id);

    List<DiaryEntryResponse> getDiaryEntries(Long profileId, LocalDate fromDate, LocalDate toDate, String keyword);

    long countDiaryEntries(Long profileId);
}
