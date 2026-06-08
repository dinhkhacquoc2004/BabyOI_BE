package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.DiaryEntryRequest;
import com.example.babyoi_be.domain.dto.respone.DiaryEntryResponse;
import com.example.babyoi_be.domain.dto.respone.DiaryImageUploadResponse;
import com.example.babyoi_be.service.DiaryEntryService;
import com.example.babyoi_be.service.DiaryImageStorageService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diary-entries")
public class DiaryEntryController {

    private final DiaryEntryService diaryEntryService;
    private final DiaryImageStorageService diaryImageStorageService;

    public DiaryEntryController(DiaryEntryService diaryEntryService, DiaryImageStorageService diaryImageStorageService) {
        this.diaryEntryService = diaryEntryService;
        this.diaryImageStorageService = diaryImageStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiaryEntryResponse createDiaryEntry(@Valid @RequestBody DiaryEntryRequest request) {
        return diaryEntryService.createDiaryEntry(request);
    }

    @PutMapping("/{id}")
    public DiaryEntryResponse updateDiaryEntry(@PathVariable Long id, @Valid @RequestBody DiaryEntryRequest request) {
        return diaryEntryService.updateDiaryEntry(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiaryEntry(@PathVariable Long id) {
        diaryEntryService.deleteDiaryEntry(id);
    }

    @GetMapping("/{id}")
    public DiaryEntryResponse getDiaryEntry(@PathVariable Long id) {
        return diaryEntryService.getDiaryEntry(id);
    }

    @GetMapping("/profile/{profileId}")
    public List<DiaryEntryResponse> getDiaryEntries(
            @PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search
    ) {
        return diaryEntryService.getDiaryEntries(profileId, fromDate, toDate, search);
    }

    @GetMapping("/profile/{profileId}/count")
    public Map<String, Long> countDiaryEntries(@PathVariable Long profileId) {
        return Map.of("count", diaryEntryService.countDiaryEntries(profileId));
    }

    @PostMapping("/image")
    public DiaryImageUploadResponse uploadDiaryImage(@RequestParam("file") MultipartFile file) {
        return new DiaryImageUploadResponse(diaryImageStorageService.uploadImage(file));
    }
}
