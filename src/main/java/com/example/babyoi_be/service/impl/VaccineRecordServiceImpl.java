package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.VaccineRuleConstants;
import com.example.babyoi_be.domain.dto.request.VaccineRecordRequest;
import com.example.babyoi_be.domain.dto.respone.VaccineRecordResponse;
import com.example.babyoi_be.domain.entity.*;
import com.example.babyoi_be.repository.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaccineRecordServiceImpl implements VaccineRecordService {

    private static final List<Long> DUPLICATE_CHECK_STATUSES = List.of(
            Constants.TABLE_STATUS.SUCCESS,
            Constants.TABLE_STATUS.PENDING
    );

    private final VaccineRecordRepository vaccineRecordRepository;
    private final VaccineRepository vaccineRepository;
    private final ChildVaccineDiseaseRepository childVaccineDiseaseRepository;
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

    private void validateUniqueVaccine(Long profileId, Long vaccineId, LocalDate injectionDate, Long currentRecordId) {
        if (vaccineId == null || injectionDate == null) {
            return;
        }

        boolean exists = currentRecordId == null
                ? vaccineRecordRepository.existsByProfileIdAndVaccineIdAndInjectionDateAndStatusIn(profileId, vaccineId, injectionDate, DUPLICATE_CHECK_STATUSES)
                : vaccineRecordRepository.existsByProfileIdAndVaccineIdAndInjectionDateAndStatusInAndIdNot(profileId, vaccineId, injectionDate, DUPLICATE_CHECK_STATUSES, currentRecordId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vaccine này đã tồn tại cho hồ sơ này");
        }
    }

    @Override
    @Transactional
    public VaccineRecordResponse createVaccineRecord(VaccineRecordRequest request) {
        validateProfileOwnership(request.getProfileId());
        validateUniqueVaccine(request.getProfileId(), request.getVaccineId(), request.getInjectionDate(), null);
        Long resolvedStatus = resolveStatusByInjectionDate(request.getStatus(), request.getInjectionDate());
        LocalDate resolvedActualInjectionDate = resolveActualInjectionDate(resolvedStatus, request.getActualInjectionDate(), request.getInjectionDate());

        VaccineRecord record = VaccineRecord.builder()
                .profileId(request.getProfileId())
                .disease(resolveDisease(request.getDiseaseId()))
                .doseOrder(request.getDoseOrder())
                .source(request.getSource() != null ? request.getSource() : VaccineRuleConstants.RECORD_SOURCE.CUSTOM)
                .vaccine(resolveVaccine(request.getVaccineId()))
                .injectionDate(request.getInjectionDate())
                .actualInjectionDate(resolvedActualInjectionDate)
                .price(request.getPrice())
                .note(request.getNote())
                .status(resolvedStatus)
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
        if (!record.getProfileId().equals(request.getProfileId())) {
            validateProfileOwnership(request.getProfileId());
        }

        validateUniqueVaccine(request.getProfileId(), request.getVaccineId(), request.getInjectionDate(), id);
        Long resolvedStatus = resolveStatusByInjectionDate(request.getStatus(), request.getInjectionDate());
        LocalDate resolvedActualInjectionDate = resolveActualInjectionDate(resolvedStatus, request.getActualInjectionDate(), request.getInjectionDate());

        record.setProfileId(request.getProfileId());
        record.setDisease(resolveDisease(request.getDiseaseId()));
        record.setDoseOrder(request.getDoseOrder());
        record.setSource(request.getSource() != null ? request.getSource() : record.getSource());
        record.setVaccine(resolveVaccine(request.getVaccineId()));
        record.setInjectionDate(request.getInjectionDate());
        record.setActualInjectionDate(resolvedActualInjectionDate);
        record.setPrice(request.getPrice());
        record.setNote(request.getNote());
        record.setStatus(resolvedStatus);
        record.setUpdatedAt(LocalDate.now());

        VaccineRecord savedRecord = vaccineRecordRepository.save(record);
        createVaccineNotification(savedRecord);
        return mapToResponse(savedRecord);
    }

    @Override
    public VaccineRecordResponse getVaccineRecordById(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine record not found"));

        validateProfileOwnership(record.getProfileId());
        return mapToResponse(record);
    }

    @Override
    @Transactional
    public void deleteCustomVaccineRecord(Long id) {
        VaccineRecord record = vaccineRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine record not found"));

        validateProfileOwnership(record.getProfileId());
        if (!VaccineRuleConstants.RECORD_SOURCE.CUSTOM.equals(record.getSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xóa hẳn mũi tiêm được thêm riêng");
        }

        vaccineRecordRepository.delete(record);
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

    private Vaccine resolveVaccine(Long vaccineId) {
        if (vaccineId == null) {
            return null;
        }

        return vaccineRepository.findById(vaccineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine not found"));
    }

    private ChildVaccineDisease resolveDisease(Long diseaseId) {
        if (diseaseId == null) {
            return null;
        }

        return childVaccineDiseaseRepository.findById(diseaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child vaccine disease not found"));
    }

    private Long resolveStatusByInjectionDate(Long requestedStatus, LocalDate injectionDate) {
        Long status = requestedStatus != null ? requestedStatus : Constants.TABLE_STATUS.PENDING;
        if (injectionDate != null && injectionDate.isAfter(LocalDate.now())) {
            return Constants.TABLE_STATUS.PENDING;
        }
        return status;
    }

    private LocalDate resolveActualInjectionDate(Long status, LocalDate requestedActualInjectionDate, LocalDate injectionDate) {
        if (!Constants.TABLE_STATUS.SUCCESS.equals(status)) {
            return null;
        }

        LocalDate actualInjectionDate = requestedActualInjectionDate != null ? requestedActualInjectionDate : injectionDate;
        if (actualInjectionDate != null && actualInjectionDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày tiêm thực tế không được ở tương lai");
        }
        return actualInjectionDate;
    }

    private VaccineRecordResponse mapToResponse(VaccineRecord record) {
        Vaccine vaccine = record.getVaccine();
        ChildVaccineDisease disease = record.getDisease();
        Long source = record.getSource() != null ? record.getSource() : VaccineRuleConstants.RECORD_SOURCE.STANDARD;

        return VaccineRecordResponse.builder()
                .id(record.getId())
                .profileId(record.getProfileId())
                .locationName(null)
                .diseaseId(disease != null ? disease.getId() : null)
                .doseOrder(record.getDoseOrder())
                .source(source)
                .sourceCode(VaccineRuleConstants.RECORD_SOURCE.codeOf(source))
                .vaccineId(vaccine != null ? vaccine.getId() : null)
                .vaccineName(vaccine != null ? vaccine.getName() : null)
                .vaccineDescription(vaccine != null ? vaccine.getDescription() : null)
                .injectionDate(record.getInjectionDate())
                .actualInjectionDate(record.getActualInjectionDate())
                .price(record.getPrice())
                .note(record.getNote())
                .status(record.getStatus())
                .build();
    }

    private void createVaccineNotification(VaccineRecord record) {
        if (!Constants.TABLE_STATUS.PENDING.equals(record.getStatus()) || record.getInjectionDate() == null) {
            return;
        }

        Profile profile = profileRepository.findById(record.getProfileId()).orElse(null);
        if (profile == null || profile.getUser() == null) {
            return;
        }

        String vaccineName = record.getVaccine() != null ? record.getVaccine().getName() : "vaccine";
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

}
