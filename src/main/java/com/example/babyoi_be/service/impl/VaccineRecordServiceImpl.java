package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.domain.entity.VaccineType;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.repository.VaccineTypeRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.NotificationService;
import com.example.babyoi_be.service.VaccineRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaccineRecordServiceImpl implements VaccineRecordService {

    private static final List<Long> DUPLICATE_CHECK_STATUSES = List.of(
            Constants.TABLE_STATUS.SUCCESS,
            Constants.TABLE_STATUS.PENDING
    );

    private final VaccineRecordRepository vaccineRecordRepository;
    private final VaccineTypeRepository vaccineTypeRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    private void validateProfileOwnership(Long profileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized");
        }

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!profile.getUser().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
    }

    private void validateUniqueVaccineType(Long profileId, Long vaccineTypeId, Long currentRecordId) {
        boolean exists = currentRecordId == null
                ? vaccineRecordRepository.existsByProfileIdAndVaccineTypeIdAndStatusIn(profileId, vaccineTypeId, DUPLICATE_CHECK_STATUSES)
                : vaccineRecordRepository.existsByProfileIdAndVaccineTypeIdAndStatusInAndIdNot(profileId, vaccineTypeId, DUPLICATE_CHECK_STATUSES, currentRecordId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loại vaccin này đã tồn tại cho hồ sơ này");
        }
    }

    @Override
    @Transactional
    public VaccineRecordResponse createVaccineRecord(VaccineRecordRequest request) {
        validateProfileOwnership(request.getProfileId());
        validateUniqueVaccineType(request.getProfileId(), request.getVaccineTypeId(), null);

        VaccineType vaccineType = vaccineTypeRepository.findById(request.getVaccineTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine type not found"));

        VaccineRecord record = VaccineRecord.builder()
                .profileId(request.getProfileId())
                .vaccineType(vaccineType)
                .injectionDate(request.getInjectionDate())
                .location(request.getLocation())
                .price(request.getPrice())
                .note(request.getNote())
                .status(request.getStatus() != null ? request.getStatus() : Constants.TABLE_STATUS.PENDING)
                .createdAt(LocalDate.now())
                .build();

        VaccineRecord savedRecord = vaccineRecordRepository.save(record);
        createVaccineNotification(savedRecord);
        return mapToResponse(savedRecord);
    }

    @Override
    @Transactional
    public VaccineRecordResponse updateVaccineRecord(Long id, VaccineRecordRequest request) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine record not found"));

        validateProfileOwnership(record.getProfileId());
        // Also validate if the new profileId in request (if different) is owned by the user
        if (!record.getProfileId().equals(request.getProfileId())) {
            validateProfileOwnership(request.getProfileId());
        }

        validateUniqueVaccineType(request.getProfileId(), request.getVaccineTypeId(), id);

        VaccineType vaccineType = vaccineTypeRepository.findById(request.getVaccineTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine type not found"));

        record.setProfileId(request.getProfileId());
        record.setVaccineType(vaccineType);
        record.setInjectionDate(request.getInjectionDate());
        record.setLocation(request.getLocation());
        record.setPrice(request.getPrice());
        record.setNote(request.getNote());
        record.setStatus(request.getStatus());
        record.setUpdatedAt(LocalDate.now());

        VaccineRecord savedRecord = vaccineRecordRepository.save(record);
        createVaccineNotification(savedRecord);
        return mapToResponse(savedRecord);
    }

    @Override
    @Transactional
    public void deleteVaccineRecord(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine record not found"));
        
        validateProfileOwnership(record.getProfileId());

        // Soft delete
        record.setStatus(Constants.TABLE_STATUS.DELETED);
        record.setUpdatedAt(LocalDate.now());
        vaccineRecordRepository.save(record);
    }

    @Override
    public VaccineRecordResponse getVaccineRecordById(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine record not found"));
        
        validateProfileOwnership(record.getProfileId());
        
        return mapToResponse(record);
    }

    @Override
    public List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status) {
        return getVaccineRecordsByStatus(profileId, status, null);
    }

    @Override
    public List<VaccineRecordResponse> getVaccineRecordsByStatus(Long profileId, Long status, Integer limit) {
        validateProfileOwnership(profileId);

        return sortByNearestToday(vaccineRecordRepository.findByProfileIdAndStatus(profileId, status))
                .stream()
                .limit(limit != null && limit > 0 ? limit : Long.MAX_VALUE)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<VaccineRecordResponse> getVaccineRecordsPage(Long profileId, Long status, Integer page, Integer size) {
        validateProfileOwnership(profileId);

        int pageIndex = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 5;
        List<VaccineRecord> sortedRecords = sortByNearestToday(vaccineRecordRepository.findByProfileIdAndStatus(profileId, status));
        int totalElements = sortedRecords.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        int fromIndex = Math.min(pageIndex * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        return PageResponse.<VaccineRecordResponse>builder()
                .content(sortedRecords.subList(fromIndex, toIndex).stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(pageIndex)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(pageIndex == 0)
                .last(totalPages == 0 || pageIndex >= totalPages - 1)
                .build();
    }

    @Override
    public long countVaccineRecordsByStatus(Long profileId, Long status) {
        validateProfileOwnership(profileId);
        return vaccineRecordRepository.countByProfileIdAndStatus(profileId, status);
    }

    @Override
    public Map<String, Long> getVaccineStatistics(Long profileId) {
        validateProfileOwnership(profileId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.PENDING));
        stats.put("canceled", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.CANCELED));
        stats.put("success", vaccineRecordRepository.countByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.SUCCESS));
        return stats;
    }

    private VaccineRecordResponse mapToResponse(VaccineRecord record) {
        VaccineType type = record.getVaccineType();
        return VaccineRecordResponse.builder()
                .id(record.getId())
                .profileId(record.getProfileId())
                .vaccineTypeId(type != null ? type.getId() : null)
                .vaccineTypeName(type != null ? type.getName() : null)
                .vaccineTypeDescription(type != null ? type.getDescription() : null)
                .injectionDate(record.getInjectionDate())
                .location(record.getLocation())
                .price(record.getPrice())
                .note(record.getNote())
                .status(record.getStatus())
                .build();
    }

    private List<VaccineRecord> sortByNearestToday(List<VaccineRecord> records) {
        LocalDate today = LocalDate.now();
        return records.stream()
                .sorted(Comparator
                        .comparingLong((VaccineRecord record) -> getDaysFromToday(record, today))
                        .thenComparing(VaccineRecord::getInjectionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VaccineRecord::getId))
                .collect(Collectors.toList());
    }

    private void createVaccineNotification(VaccineRecord record) {
        if (!Constants.TABLE_STATUS.PENDING.equals(record.getStatus()) || record.getInjectionDate() == null) {
            return;
        }

        Profile profile = profileRepository.findById(record.getProfileId()).orElse(null);
        if (profile == null || profile.getUser() == null) {
            return;
        }

        String vaccineName = record.getVaccineType() != null ? record.getVaccineType().getName() : "vaccine";
        String profileName = profile.getName() != null ? profile.getName() : "bé";
        boolean isToday = LocalDate.now().equals(record.getInjectionDate());
        String title = isToday ? "Lịch tiêm hôm nay" : "Nhắc lịch tiêm";
        String body = isToday
                ? "Bé " + profileName + " có lịch tiêm hôm nay: mũi " + vaccineName + "."
                : "Bé " + profileName + " sắp có lịch tiêm ngày " + record.getInjectionDate() + ": mũi " + vaccineName + ".";
        String dataJson = String.format(
                "{\"screen\":\"VaccineRecordDetail\",\"recordId\":%d,\"profileId\":%d}",
                record.getId(),
                record.getProfileId()
        );

        notificationService.createNotification(
                profile.getUser().getId(),
                Constants.NOTIFICATION_TYPE.VACCINE_REMINDER,
                title,
                body,
                dataJson,
                Constants.NOTIFICATION_PRIORITY.HIGH,
                "VACCINE_RECORD",
                record.getId(),
                true
        );
    }

    private long getDaysFromToday(VaccineRecord record, LocalDate today) {
        if (record.getInjectionDate() == null) {
            return Long.MAX_VALUE;
        }

        return Math.abs(ChronoUnit.DAYS.between(today, record.getInjectionDate()));
    }
}
