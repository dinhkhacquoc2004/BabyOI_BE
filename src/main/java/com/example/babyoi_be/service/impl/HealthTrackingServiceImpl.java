package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.HealthRecordRequest;
import com.example.babyoi_be.domain.dto.request.IllnessEventRequest;
import com.example.babyoi_be.domain.dto.respone.HealthMonthlyDetailResponse;
import com.example.babyoi_be.domain.dto.respone.HealthRecordResponse;
import com.example.babyoi_be.domain.dto.respone.IllnessEventResponse;
import com.example.babyoi_be.domain.entity.HealthRecord;
import com.example.babyoi_be.domain.entity.IllnessEvent;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.repository.HealthRecordRepository;
import com.example.babyoi_be.repository.IllnessEventRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.HealthTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthTrackingServiceImpl implements HealthTrackingService {

    private static final long ILLNESS_STATUS_LIGHT = 1L;
    private static final long ILLNESS_STATUS_NORMAL = 2L;
    private static final long ILLNESS_STATUS_ATTENTION = 3L;

    private final HealthRecordRepository healthRecordRepository;
    private final IllnessEventRepository illnessEventRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public HealthRecordResponse createHealthRecord(HealthRecordRequest request) {
        Profile profile = resolveProfile(request.getProfileId());
        validateHealthRecordRequest(request);

        LocalDateTime now = LocalDateTime.now();
        HealthRecord record = HealthRecord.builder()
                .profile(profile)
                .height(request.getHeight())
                .weight(request.getWeight())
                .bmi(resolveBmi(request.getHeight(), request.getWeight(), request.getBmi()))
                .recordDate(request.getRecordDate())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return mapHealthRecord(healthRecordRepository.save(record));
    }

    @Override
    @Transactional
    public HealthRecordResponse updateHealthRecord(Long id, HealthRecordRequest request) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found"));
        Profile profile = resolveProfile(request.getProfileId());
        validateHealthRecordRequest(request);

        record.setProfile(profile);
        record.setHeight(request.getHeight());
        record.setWeight(request.getWeight());
        record.setBmi(resolveBmi(request.getHeight(), request.getWeight(), request.getBmi()));
        record.setRecordDate(request.getRecordDate());
        record.setUpdatedAt(LocalDateTime.now());

        return mapHealthRecord(healthRecordRepository.save(record));
    }

    @Override
    @Transactional
    public void deleteHealthRecord(Long id) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found"));
        validateCurrentUser(record.getProfile().getUser().getId());
        healthRecordRepository.delete(record);
    }

    @Override
    public HealthRecordResponse getHealthRecord(Long id) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found"));
        validateCurrentUser(record.getProfile().getUser().getId());
        return mapHealthRecord(record);
    }

    @Override
    public List<HealthRecordResponse> getHealthRecords(Long profileId, LocalDate fromDate, LocalDate toDate, Integer month, Integer year) {
        Profile profile = resolveProfile(profileId);
        DateRange range = resolveDateRange(fromDate, toDate, month, year);

        return healthRecordRepository.findByProfileIdAndRecordDateBetweenOrderByRecordDateDesc(profile.getId(), range.fromDate(), range.toDate())
                .stream()
                .map(this::mapHealthRecord)
                .toList();
    }

    @Override
    public List<HealthRecordResponse> getDevelopmentHistory(Long profileId, LocalDate fromDate, LocalDate toDate, Integer month, Integer year) {
        Profile profile = resolveProfile(profileId);
        DateRange range = resolveDateRange(fromDate, toDate, month, year);

        return healthRecordRepository.findByProfileIdAndRecordDateBetweenOrderByRecordDateAsc(profile.getId(), range.fromDate(), range.toDate())
                .stream()
                .map(this::mapHealthRecord)
                .toList();
    }

    @Override
    public HealthMonthlyDetailResponse getMonthlyDetail(Long profileId, Integer month, Integer year) {
        Profile profile = resolveProfile(profileId);
        YearMonth yearMonth = YearMonth.of(resolveYear(year), resolveMonth(month));
        LocalDate fromDate = yearMonth.atDay(1);
        LocalDate toDate = yearMonth.atEndOfMonth();

        HealthRecordResponse healthRecord = healthRecordRepository
                .findFirstByProfileIdAndRecordDateBetweenOrderByRecordDateDesc(profile.getId(), fromDate, toDate)
                .map(this::mapHealthRecord)
                .orElse(null);
        List<IllnessEventResponse> illnesses = illnessEventRepository
                .findByProfileAndDateRange(profile.getId(), fromDate, toDate, null)
                .stream()
                .map(this::mapIllnessEvent)
                .toList();

        return new HealthMonthlyDetailResponse(healthRecord, illnesses);
    }

    @Override
    @Transactional
    public IllnessEventResponse createIllnessEvent(IllnessEventRequest request) {
        Profile profile = resolveProfile(request.getProfileId());
        validateIllnessEventRequest(request);

        LocalDateTime now = LocalDateTime.now();
        IllnessEvent event = IllnessEvent.builder()
                .profile(profile)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .illnessType(request.getIllnessType().trim())
                .status(resolveIllnessStatus(request.getStatus()))
                .description(normalizeText(request.getDescription()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return mapIllnessEvent(illnessEventRepository.save(event));
    }

    @Override
    @Transactional
    public IllnessEventResponse updateIllnessEvent(Long id, IllnessEventRequest request) {
        IllnessEvent event = illnessEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Illness event not found"));
        Profile profile = resolveProfile(request.getProfileId());
        validateIllnessEventRequest(request);

        event.setProfile(profile);
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setIllnessType(request.getIllnessType().trim());
        event.setStatus(resolveIllnessStatus(request.getStatus()));
        event.setDescription(normalizeText(request.getDescription()));
        event.setUpdatedAt(LocalDateTime.now());

        return mapIllnessEvent(illnessEventRepository.save(event));
    }

    @Override
    @Transactional
    public void deleteIllnessEvent(Long id) {
        IllnessEvent event = illnessEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Illness event not found"));
        validateCurrentUser(event.getProfile().getUser().getId());
        illnessEventRepository.delete(event);
    }

    @Override
    public IllnessEventResponse getIllnessEvent(Long id) {
        IllnessEvent event = illnessEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Illness event not found"));
        validateCurrentUser(event.getProfile().getUser().getId());
        return mapIllnessEvent(event);
    }

    @Override
    public List<IllnessEventResponse> getIllnessEvents(Long profileId, LocalDate fromDate, LocalDate toDate, Integer month, Integer year, Long status) {
        Profile profile = resolveProfile(profileId);
        DateRange range = resolveDateRange(fromDate, toDate, month, year);

        return illnessEventRepository.findByProfileAndDateRange(profile.getId(), range.fromDate(), range.toDate(), status)
                .stream()
                .map(this::mapIllnessEvent)
                .toList();
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

    private void validateHealthRecordRequest(HealthRecordRequest request) {
        if (request.getRecordDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Record date cannot be in the future");
        }
        if (request.getHeight() == null && request.getWeight() == null && request.getBmi() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one health index is required");
        }
    }

    private void validateIllnessEventRequest(IllnessEventRequest request) {
        if (request.getStartAt().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be in the future");
        }
        if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }

    private Double resolveBmi(Double height, Double weight, Double requestedBmi) {
        if (requestedBmi != null) {
            return roundOneDecimal(requestedBmi);
        }
        if (height == null || weight == null || height <= 0 || weight <= 0) {
            return null;
        }

        double heightMeters = height / 100.0;
        return roundOneDecimal(weight / (heightMeters * heightMeters));
    }

    private Double roundOneDecimal(Double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Long resolveIllnessStatus(Long status) {
        if (status == null) {
            return ILLNESS_STATUS_NORMAL;
        }
        if (status.equals(ILLNESS_STATUS_LIGHT) || status.equals(ILLNESS_STATUS_NORMAL) || status.equals(ILLNESS_STATUS_ATTENTION)) {
            return status;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illness status must be 1, 2 or 3");
    }

    private DateRange resolveDateRange(LocalDate fromDate, LocalDate toDate, Integer month, Integer year) {
        if (month != null || year != null) {
            if (year != null && month == null) {
                int resolvedYear = resolveYear(year);
                return new DateRange(LocalDate.of(resolvedYear, 1, 1), LocalDate.of(resolvedYear, 12, 31));
            }
            YearMonth yearMonth = YearMonth.of(resolveYear(year), resolveMonth(month));
            return new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        }

        LocalDate resolvedFromDate = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
        LocalDate resolvedToDate = toDate != null ? toDate : LocalDate.of(2999, 12, 31);
        if (resolvedToDate.isBefore(resolvedFromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To date cannot be before from date");
        }

        return new DateRange(resolvedFromDate, resolvedToDate);
    }

    private int resolveMonth(Integer month) {
        int resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        }
        return resolvedMonth;
    }

    private int resolveYear(Integer year) {
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        if (resolvedYear < 1900 || resolvedYear > 2999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year is not valid");
        }
        return resolvedYear;
    }

    private HealthRecordResponse mapHealthRecord(HealthRecord record) {
        LocalDate monthStart = record.getRecordDate().withDayOfMonth(1);
        LocalDate monthEnd = YearMonth.from(record.getRecordDate()).atEndOfMonth();
        long illnessCount = illnessEventRepository.countByProfileAndDateRange(record.getProfile().getId(), monthStart, monthEnd);

        return HealthRecordResponse.builder()
                .id(record.getId())
                .profileId(record.getProfile().getId())
                .height(record.getHeight())
                .weight(record.getWeight())
                .bmi(record.getBmi())
                .illnessCount(illnessCount)
                .recordDate(record.getRecordDate())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private IllnessEventResponse mapIllnessEvent(IllnessEvent event) {
        return IllnessEventResponse.builder()
                .id(event.getId())
                .profileId(event.getProfile().getId())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .illnessType(event.getIllnessType())
                .status(event.getStatus())
                .statusLabel(resolveIllnessStatusLabel(event.getStatus()))
                .description(event.getDescription())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private String resolveIllnessStatusLabel(Long status) {
        if (ILLNESS_STATUS_LIGHT == (status == null ? ILLNESS_STATUS_NORMAL : status)) {
            return "Nhẹ";
        }
        if (ILLNESS_STATUS_ATTENTION == (status == null ? ILLNESS_STATUS_NORMAL : status)) {
            return "Cần chú ý";
        }
        return "Bình thường";
    }

    private String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
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
