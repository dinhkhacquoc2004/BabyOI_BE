package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.DiaryEntryRequest;
import com.example.babyoi_be.domain.dto.respone.DiaryEntryResponse;
import com.example.babyoi_be.domain.entity.DiaryEntry;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.repository.DiaryEntryRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.DiaryEntryService;
import com.example.babyoi_be.service.DiaryImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryEntryServiceImpl implements DiaryEntryService {

    private final DiaryEntryRepository diaryEntryRepository;
    private final ProfileRepository profileRepository;
    private final DiaryImageStorageService diaryImageStorageService;

    @Override
    @Transactional
    public DiaryEntryResponse createDiaryEntry(DiaryEntryRequest request) {
        Profile profile = resolveProfile(request.getProfileId());
        validateDiaryEntryRequest(request);

        LocalDateTime now = LocalDateTime.now();
        DiaryEntry entry = DiaryEntry.builder()
                .profile(profile)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .imageUrl(normalizeRemoteImageUrl(request.getImageUrl()))
                .milestone(normalizeText(request.getMilestone()))
                .entryDate(request.getEntryDate())
                .status(Constants.TABLE_STATUS.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return mapDiaryEntry(diaryEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public DiaryEntryResponse updateDiaryEntry(Long id, DiaryEntryRequest request) {
        DiaryEntry entry = diaryEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary entry not found"));
        validateEntryActive(entry);
        validateCurrentUser(entry.getProfile().getUser().getId());

        Profile profile = resolveProfile(request.getProfileId());
        validateDiaryEntryRequest(request);

        String oldImageUrl = entry.getImageUrl();
        String newImageUrl = normalizeRemoteImageUrl(request.getImageUrl());

        entry.setProfile(profile);
        entry.setTitle(request.getTitle().trim());
        entry.setContent(request.getContent().trim());
        entry.setImageUrl(newImageUrl);
        entry.setMilestone(normalizeText(request.getMilestone()));
        entry.setEntryDate(request.getEntryDate());
        entry.setUpdatedAt(LocalDateTime.now());

        DiaryEntry savedEntry = diaryEntryRepository.save(entry);
        deleteOldImageIfChanged(oldImageUrl, newImageUrl);
        return mapDiaryEntry(savedEntry);
    }

    @Override
    @Transactional
    public void deleteDiaryEntry(Long id) {
        DiaryEntry entry = diaryEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary entry not found"));
        validateEntryActive(entry);
        validateCurrentUser(entry.getProfile().getUser().getId());

        String imageUrl = entry.getImageUrl();
        entry.setStatus(Constants.TABLE_STATUS.DELETED);
        entry.setUpdatedAt(LocalDateTime.now());
        diaryEntryRepository.save(entry);
        diaryImageStorageService.deleteImage(imageUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public DiaryEntryResponse getDiaryEntry(Long id) {
        DiaryEntry entry = diaryEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary entry not found"));
        validateEntryActive(entry);
        validateCurrentUser(entry.getProfile().getUser().getId());
        return mapDiaryEntry(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiaryEntryResponse> getDiaryEntries(Long profileId, LocalDate fromDate, LocalDate toDate, String keyword) {
        Profile profile = resolveProfile(profileId);
        DateRange range = resolveDateRange(fromDate, toDate);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<DiaryEntry> entries = normalizedKeyword == null
                ? diaryEntryRepository.findByProfileIdAndStatusAndEntryDateBetweenOrderByEntryDateDescCreatedAtDescIdDesc(
                        profile.getId(),
                        Constants.TABLE_STATUS.ACTIVE,
                        range.fromDate(),
                        range.toDate()
                )
                : diaryEntryRepository.searchActiveEntries(
                        profile.getId(),
                        Constants.TABLE_STATUS.ACTIVE,
                        range.fromDate(),
                        range.toDate(),
                        normalizedKeyword
                );

        return entries
                .stream()
                .map(this::mapDiaryEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDiaryEntries(Long profileId) {
        Profile profile = resolveProfile(profileId);
        return diaryEntryRepository.countByProfileIdAndStatus(profile.getId(), Constants.TABLE_STATUS.ACTIVE);
    }

    private Profile resolveProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!Constants.TABLE_STATUS.ACTIVE.equals(profile.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile is not active");
        }
        validateCurrentUser(profile.getUser().getId());
        return profile;
    }

    private void validateDiaryEntryRequest(DiaryEntryRequest request) {
        if (request.getEntryDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry date cannot be in the future");
        }
        if (normalizeRemoteImageUrl(request.getImageUrl()) == null && request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL is not valid");
        }
    }

    private void validateEntryActive(DiaryEntry entry) {
        if (!Constants.TABLE_STATUS.ACTIVE.equals(entry.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary entry not found");
        }
    }

    private DateRange resolveDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedFromDate = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
        LocalDate resolvedToDate = toDate != null ? toDate : LocalDate.of(2999, 12, 31);
        if (resolvedToDate.isBefore(resolvedFromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To date cannot be before from date");
        }

        return new DateRange(resolvedFromDate, resolvedToDate);
    }

    private DiaryEntryResponse mapDiaryEntry(DiaryEntry entry) {
        return DiaryEntryResponse.builder()
                .id(entry.getId())
                .profileId(entry.getProfile().getId())
                .title(entry.getTitle())
                .content(entry.getContent())
                .imageUrl(entry.getImageUrl())
                .milestone(entry.getMilestone())
                .entryDate(entry.getEntryDate())
                .status(entry.getStatus())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private String normalizeRemoteImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String normalizedImageUrl = imageUrl.trim();
        if (normalizedImageUrl.startsWith("http://") || normalizedImageUrl.startsWith("https://")) {
            return normalizedImageUrl;
        }

        return null;
    }

    private String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private void deleteOldImageIfChanged(String oldImageUrl, String newImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            return;
        }
        if (newImageUrl != null && oldImageUrl.trim().equals(newImageUrl.trim())) {
            return;
        }
        diaryImageStorageService.deleteImage(oldImageUrl);
    }

    private void validateCurrentUser(Long userId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return;
        }
        if (!userDetails.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }
}
