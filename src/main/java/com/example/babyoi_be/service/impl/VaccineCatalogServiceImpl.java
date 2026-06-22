package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.VaccineRuleConstants;
import com.example.babyoi_be.common.utils.TextSearchUtils;
import com.example.babyoi_be.domain.dto.respone.*;
import com.example.babyoi_be.domain.entity.*;
import com.example.babyoi_be.repository.*;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.VaccineCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaccineCatalogServiceImpl implements VaccineCatalogService {
    private static final List<Long> PROGRESS_STATUSES = List.of(
            Constants.TABLE_STATUS.SUCCESS,
            Constants.TABLE_STATUS.PENDING,
            Constants.TABLE_STATUS.INACTIVE,
            Constants.TABLE_STATUS.CANCELED
    );

    private final VaccineRepository vaccineRepository;
    private final ChildVaccineDiseaseRepository childVaccineDiseaseRepository;
    private final VaccineDiseaseCoverageRepository vaccineDiseaseCoverageRepository;
    private final ChildDiseaseDoseScheduleRepository childDiseaseDoseScheduleRepository;
    private final ChildDiseaseDoseVaccineOptionRepository childDiseaseDoseVaccineOptionRepository;
    private final ProfileVaccineDiseaseStatusRepository profileVaccineDiseaseStatusRepository;
    private final VaccineRecordRepository vaccineRecordRepository;
    private final ProfileRepository profileRepository;

    @Override
    public List<VaccineResponse> getVaccines(String keyword) {
        return vaccineRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(vaccine -> keyword == null || keyword.trim().isEmpty() || TextSearchUtils.contains(vaccine.getName(), keyword))
                .map(this::mapVaccine)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChildVaccineDiseaseResponse> getChildVaccineDiseases() {
        return childVaccineDiseaseRepository.findByStatusOrderByDisplayOrderAscIdAsc(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapChildDisease)
                .collect(Collectors.toList());
    }

    private List<VaccineDiseaseCoverageResponse> getVaccineDiseaseCoverages(Long diseaseId, Long vaccineId) {
        List<VaccineDiseaseCoverage> coverages;
        if (diseaseId != null) {
            coverages = vaccineDiseaseCoverageRepository.findByDiseaseIdAndStatus(diseaseId, Constants.TABLE_STATUS.ACTIVE);
        } else if (vaccineId != null) {
            coverages = vaccineDiseaseCoverageRepository.findByVaccineIdAndStatus(vaccineId, Constants.TABLE_STATUS.ACTIVE);
        } else {
            coverages = vaccineDiseaseCoverageRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE);
        }

        return coverages.stream()
                .map(this::mapCoverage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChildDiseaseDoseVaccineOptionResponse> getChildDiseaseDoseVaccineOptions(Long diseaseId) {
        return childDiseaseDoseVaccineOptionRepository
                .findByDiseaseIdAndStatusOrderByDoseOrderAscDisplayOrderAscIdAsc(diseaseId, Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapChildDoseVaccineOption)
                .collect(Collectors.toList());
    }

    @Override
    public ChildDiseasePlanResponse getChildDiseasePlan(Long diseaseId) {
        ChildVaccineDisease disease = childVaccineDiseaseRepository.findById(diseaseId)
                .filter(item -> Constants.TABLE_STATUS.ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child vaccine disease not found"));

        List<ChildDiseaseDoseSchedule> schedules = childDiseaseDoseScheduleRepository
                .findByDiseaseIdAndStatusOrderByDoseOrderAsc(diseaseId, Constants.TABLE_STATUS.ACTIVE);
        Map<Integer, List<ChildDiseaseDoseVaccineOptionResponse>> optionsByDoseOrder = childDiseaseDoseVaccineOptionRepository
                .findByDiseaseIdAndStatusOrderByDoseOrderAscDisplayOrderAscIdAsc(diseaseId, Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapChildDoseVaccineOption)
                .collect(Collectors.groupingBy(ChildDiseaseDoseVaccineOptionResponse::getDoseOrder, LinkedHashMap::new, Collectors.toList()));

        List<ChildDiseaseDosePlanResponse> doses = schedules.stream()
                .map(schedule -> ChildDiseaseDosePlanResponse.builder()
                        .schedule(mapChildDoseSchedule(schedule))
                        .vaccineOptions(optionsByDoseOrder.getOrDefault(schedule.getDoseOrder(), List.of()))
                        .build())
                .collect(Collectors.toList());

        return ChildDiseasePlanResponse.builder()
                .disease(mapChildDisease(disease))
                .doses(doses)
                .coverages(getVaccineDiseaseCoverages(diseaseId, null))
                .build();
    }

    @Override
    public List<VaccineProgressResponse> getVaccineProgress(Long profileId) {
        validateProfileOwnership(profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        List<ChildVaccineDisease> diseases = childVaccineDiseaseRepository.findByStatusOrderByDisplayOrderAscIdAsc(Constants.TABLE_STATUS.ACTIVE);
        Map<Long, ProfileVaccineDiseaseStatus> statusByDiseaseId = profileVaccineDiseaseStatusRepository.findByProfileId(profileId)
                .stream()
                .filter(status -> status.getDisease() != null && status.getDisease().getId() != null)
                .collect(Collectors.toMap(status -> status.getDisease().getId(), status -> status, (first, ignored) -> first));
        return buildDiseaseProgress(profile, diseases, statusByDiseaseId);
    }

    @Override
    @Transactional
    public VaccineProgressResponse stopProfileDiseaseSchedule(Long profileId, Long diseaseId) {
        validateProfileOwnership(profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        ChildVaccineDisease disease = childVaccineDiseaseRepository.findById(diseaseId)
                .filter(item -> Constants.TABLE_STATUS.ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child vaccine disease not found"));

        VaccineProgressResponse currentProgress = buildDiseaseProgress(profile, List.of(disease), Map.of())
                .stream()
                .findFirst()
                .orElse(null);

        ProfileVaccineDiseaseStatus diseaseStatus = profileVaccineDiseaseStatusRepository
                .findByProfileIdAndDiseaseId(profileId, diseaseId)
                .orElseGet(() -> ProfileVaccineDiseaseStatus.builder()
                        .profile(profile)
                        .disease(disease)
                        .createdAt(LocalDateTime.now())
                        .build());

        LocalDateTime now = LocalDateTime.now();
        diseaseStatus.setStatus(VaccineRuleConstants.DISEASE_SCHEDULE_STATUS.STOPPED);
        diseaseStatus.setStoppedAt(now);
        diseaseStatus.setStoppedDoseOrder(currentProgress != null ? currentProgress.getNextDoseOrder() : null);
        diseaseStatus.setUpdatedAt(now);
        ProfileVaccineDiseaseStatus savedStatus = profileVaccineDiseaseStatusRepository.save(diseaseStatus);

        return buildDiseaseProgress(profile, List.of(disease), Map.of(diseaseId, savedStatus))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot build disease progress"));
    }

    @Override
    @Transactional
    public VaccineProgressResponse resumeProfileDiseaseSchedule(Long profileId, Long diseaseId) {
        validateProfileOwnership(profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        ChildVaccineDisease disease = childVaccineDiseaseRepository.findById(diseaseId)
                .filter(item -> Constants.TABLE_STATUS.ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child vaccine disease not found"));

        ProfileVaccineDiseaseStatus diseaseStatus = profileVaccineDiseaseStatusRepository
                .findByProfileIdAndDiseaseId(profileId, diseaseId)
                .orElseGet(() -> ProfileVaccineDiseaseStatus.builder()
                        .profile(profile)
                        .disease(disease)
                        .createdAt(LocalDateTime.now())
                        .build());

        diseaseStatus.setStatus(VaccineRuleConstants.DISEASE_SCHEDULE_STATUS.ACTIVE);
        diseaseStatus.setStoppedAt(null);
        diseaseStatus.setStoppedDoseOrder(null);
        diseaseStatus.setUpdatedAt(LocalDateTime.now());
        ProfileVaccineDiseaseStatus savedStatus = profileVaccineDiseaseStatusRepository.save(diseaseStatus);

        return buildDiseaseProgress(profile, List.of(disease), Map.of(diseaseId, savedStatus))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot build disease progress"));
    }

    private List<VaccineProgressResponse> buildDiseaseProgress(
            Profile profile,
            List<ChildVaccineDisease> diseases,
            Map<Long, ProfileVaccineDiseaseStatus> statusByDiseaseId
    ) {
        List<VaccineRecord> records = vaccineRecordRepository.findByProfileId(profile.getId())
                .stream()
                .filter(record -> PROGRESS_STATUSES.contains(record.getStatus()))
                .collect(Collectors.toList());

        Set<Long> vaccineIds = records.stream()
                .filter(record -> record.getVaccine() != null && record.getVaccine().getId() != null)
                .map(record -> record.getVaccine().getId())
                .collect(Collectors.toSet());
        Map<Long, List<VaccineDiseaseCoverage>> coveragesByVaccineId = vaccineIds.isEmpty()
                ? Map.of()
                : vaccineDiseaseCoverageRepository.findByVaccineIdInAndStatus(vaccineIds, Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(coverage -> coverage.getVaccine().getId()));
        Map<Long, List<ChildDiseaseDoseSchedule>> schedulesByDiseaseId = childDiseaseDoseScheduleRepository
                .findByStatusOrderByDiseaseDisplayOrderAscDoseOrderAsc(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getDisease().getId()));

        Map<Long, List<VaccineRecord>> recordsByDiseaseId = new HashMap<>();
        Map<Long, List<VaccineDiseaseCoverage>> usedCoveragesByDiseaseId = new HashMap<>();
        for (VaccineRecord record : records) {
            if (record.getDisease() != null && record.getDisease().getId() != null) {
                recordsByDiseaseId.computeIfAbsent(record.getDisease().getId(), ignored -> new ArrayList<>()).add(record);
            }
            if (record.getVaccine() == null || record.getVaccine().getId() == null) {
                continue;
            }
            List<VaccineDiseaseCoverage> coverages = coveragesByVaccineId.getOrDefault(record.getVaccine().getId(), List.of());
            for (VaccineDiseaseCoverage coverage : coverages) {
                ChildVaccineDisease disease = coverage.getDisease();
                if (disease == null || disease.getId() == null) {
                    continue;
                }
                List<ChildDiseaseDoseSchedule> targetSchedules = schedulesByDiseaseId.getOrDefault(disease.getId(), List.of());
                List<ChildDiseaseDoseSchedule> sourceSchedules = record.getDisease() != null && record.getDisease().getId() != null
                        ? schedulesByDiseaseId.getOrDefault(record.getDisease().getId(), List.of())
                        : List.of();
                if (record.getDisease() == null || record.getDisease().getId() == null) {
                    recordsByDiseaseId.computeIfAbsent(disease.getId(), ignored -> new ArrayList<>())
                            .add(copyRecordForCoveredDisease(record, disease, targetSchedules, sourceSchedules, profile.getDateOfBirth()));
                } else if (!disease.getId().equals(record.getDisease().getId())) {
                    recordsByDiseaseId.computeIfAbsent(disease.getId(), ignored -> new ArrayList<>())
                            .add(copyRecordForCoveredDisease(record, disease, targetSchedules, sourceSchedules, profile.getDateOfBirth()));
                }
                usedCoveragesByDiseaseId.computeIfAbsent(disease.getId(), ignored -> new ArrayList<>()).add(coverage);
            }
        }

        return diseases.stream()
                .map(disease -> buildDiseaseProgressItem(
                        disease,
                        recordsByDiseaseId.getOrDefault(disease.getId(), List.of()),
                        usedCoveragesByDiseaseId.getOrDefault(disease.getId(), List.of()),
                        schedulesByDiseaseId.getOrDefault(disease.getId(), List.of()),
                        profile.getDateOfBirth(),
                        statusByDiseaseId.get(disease.getId())
                ))
                .sorted(Comparator.comparing((VaccineProgressResponse response) -> Boolean.TRUE.equals(response.getStopped()) ? 1 : 0)
                        .thenComparing(VaccineProgressResponse::getNextInjectionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VaccineProgressResponse::getDiseaseName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    private VaccineProgressResponse buildDiseaseProgressItem(
            ChildVaccineDisease disease,
            List<VaccineRecord> records,
            List<VaccineDiseaseCoverage> coverages,
            List<ChildDiseaseDoseSchedule> schedules,
            LocalDate dateOfBirth,
            ProfileVaccineDiseaseStatus diseaseStatus
    ) {
        List<VaccineRecord> sortedRecords = records.stream()
                .distinct()
                .sorted(Comparator
                        .comparing(VaccineRecord::getInjectionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VaccineRecord::getId))
                .collect(Collectors.toList());
        List<VaccineRecord> visibleRecords = sortedRecords.stream()
                .filter(record -> !Constants.TABLE_STATUS.CANCELED.equals(record.getStatus()))
                .collect(Collectors.toList());

        int scheduledDoses = schedules.stream()
                .map(ChildDiseaseDoseSchedule::getDoseOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int recordedMaxDoseOrder = visibleRecords.stream()
                .map(VaccineRecord::getDoseOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int totalDoses = Math.max(Math.max(scheduledDoses, recordedMaxDoseOrder), visibleRecords.size());
        Long interchangeRule = coverages.stream()
                .map(VaccineDiseaseCoverage::getInterchangeRule)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(VaccineRuleConstants.INTERCHANGE_RULE.PREFER_SAME_PRODUCT);
        List<String> families = coverages.stream()
                .map(VaccineDiseaseCoverage::getProductFamilyCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        List<VaccineProgressDoseResponse> doses = buildDiseaseProgressDoses(sortedRecords, schedules, totalDoses, dateOfBirth);
        int visibleTotalDoses = doses.size();
        int completedDoses = (int) doses.stream()
                .filter(dose -> isCompletedForProgress(dose.getStatus()))
                .count();
        int pendingDoses = (int) doses.stream()
                .filter(dose -> Constants.TABLE_STATUS.PENDING.equals(dose.getStatus()))
                .count();
        LocalDate today = LocalDate.now();
        int overdueDoses = (int) doses.stream()
                .filter(dose -> Constants.TABLE_STATUS.PENDING.equals(dose.getStatus()))
                .filter(dose -> dose.getInjectionDate() != null && dose.getInjectionDate().isBefore(today))
                .count();
        int upcomingDoses = pendingDoses - overdueDoses;
        Integer nextDoseOrder = doses.stream()
                .filter(dose -> Constants.TABLE_STATUS.PENDING.equals(dose.getStatus()))
                .map(VaccineProgressDoseResponse::getDoseOrder)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        LocalDate nextInjectionDate = doses.stream()
                .filter(dose -> Constants.TABLE_STATUS.PENDING.equals(dose.getStatus()))
                .map(VaccineProgressDoseResponse::getInjectionDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
        boolean stopped = diseaseStatus != null
                && VaccineRuleConstants.DISEASE_SCHEDULE_STATUS.STOPPED.equals(diseaseStatus.getStatus());

        return VaccineProgressResponse.builder()
                .diseaseId(disease.getId())
                .diseaseCode(disease.getCode())
                .diseaseName(disease.getName())
                .groupId(disease.getId())
                .groupCode(disease.getCode())
                .groupName(disease.getName())
                .totalDoses(visibleTotalDoses)
                .completedDoses(completedDoses)
                .pendingDoses(pendingDoses)
                .overdueDoses(overdueDoses)
                .upcomingDoses(upcomingDoses)
                .currentDoseOrder(completedDoses)
                .nextDoseOrder(nextDoseOrder)
                .nextInjectionDate(nextInjectionDate)
                .stopped(stopped)
                .diseaseScheduleStatus(stopped ? diseaseStatus.getStatus() : VaccineRuleConstants.DISEASE_SCHEDULE_STATUS.ACTIVE)
                .stoppedAt(stopped ? diseaseStatus.getStoppedAt() : null)
                .stoppedDoseOrder(stopped ? diseaseStatus.getStoppedDoseOrder() : null)
                .interchangeRule(interchangeRule)
                .interchangeRuleCode(VaccineRuleConstants.INTERCHANGE_RULE.codeOf(interchangeRule))
                .productFamilyCodes(families)
                .warnings(buildRuleWarnings(interchangeRule))
                .doses(doses)
                .build();
    }

    private List<VaccineProgressDoseResponse> buildDiseaseProgressDoses(
            List<VaccineRecord> records,
            List<ChildDiseaseDoseSchedule> schedules,
            int totalDoses,
            LocalDate dateOfBirth
    ) {
        Map<Integer, ChildDiseaseDoseSchedule> scheduleByDoseOrder = schedules.stream()
                .filter(schedule -> schedule.getDoseOrder() != null)
                .collect(Collectors.toMap(ChildDiseaseDoseSchedule::getDoseOrder, schedule -> schedule, (first, ignored) -> first));
        Map<Integer, VaccineRecord> recordByDoseOrder = records.stream()
                .filter(record -> record.getDoseOrder() != null)
                .collect(Collectors.toMap(VaccineRecord::getDoseOrder, record -> record, this::preferVisibleRecord));
        List<VaccineRecord> recordsWithoutDoseOrder = records.stream()
                .filter(record -> record.getDoseOrder() == null)
                .collect(Collectors.toList());
        List<VaccineProgressDoseResponse> responses = new ArrayList<>();
        int fallbackRecordIndex = 0;
        for (int doseOrder = 1; doseOrder <= totalDoses; doseOrder++) {
            VaccineRecord record = recordByDoseOrder.get(doseOrder);
            if (record == null && fallbackRecordIndex < recordsWithoutDoseOrder.size()) {
                record = recordsWithoutDoseOrder.get(fallbackRecordIndex);
                fallbackRecordIndex++;
            }
            ChildDiseaseDoseSchedule schedule = scheduleByDoseOrder.get(doseOrder);
            if (record != null) {
                if (Constants.TABLE_STATUS.CANCELED.equals(record.getStatus())) {
                    continue;
                }
                Vaccine vaccine = record.getVaccine();
                Long source = record.getSource() != null ? record.getSource() : VaccineRuleConstants.RECORD_SOURCE.STANDARD;
                responses.add(VaccineProgressDoseResponse.builder()
                        .recordId(record.getId())
                        .doseOrder(doseOrder)
                        .source(source)
                        .sourceCode(VaccineRuleConstants.RECORD_SOURCE.codeOf(source))
                        .vaccineId(vaccine != null ? vaccine.getId() : null)
                        .vaccineName(vaccine != null ? vaccine.getName() : null)
                        .manufacturer(vaccine != null ? vaccine.getManufacturer() : null)
                        .injectionDate(record.getInjectionDate())
                        .actualInjectionDate(record.getActualInjectionDate())
                        .status(record.getStatus())
                        .overdue(isOverdue(record.getStatus(), record.getInjectionDate()))
                        .note(record.getNote())
                        .build());
                continue;
            }

            if (schedule == null) {
                continue;
            }

            LocalDate expectedDate = null;
            if (dateOfBirth != null && schedule != null && schedule.getRecommendedAgeMonths() != null) {
                expectedDate = dateOfBirth.plusMonths(schedule.getRecommendedAgeMonths());
            }
            Long generatedStatus = expectedDate != null && expectedDate.isBefore(LocalDate.now())
                    ? Constants.TABLE_STATUS.INACTIVE
                    : Constants.TABLE_STATUS.PENDING;
            responses.add(VaccineProgressDoseResponse.builder()
                    .recordId(null)
                    .doseOrder(doseOrder)
                    .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                    .sourceCode(VaccineRuleConstants.RECORD_SOURCE.codeOf(VaccineRuleConstants.RECORD_SOURCE.STANDARD))
                    .vaccineId(null)
                    .vaccineName(schedule != null ? schedule.getDoseLabel() : null)
                    .manufacturer(null)
                    .injectionDate(expectedDate)
                    .actualInjectionDate(null)
                    .status(generatedStatus)
                    .overdue(isOverdue(generatedStatus, expectedDate))
                    .note(schedule != null ? schedule.getNote() : null)
                    .build());
        }
        return responses;
    }

    private boolean isCompletedForProgress(Long status) {
        return Constants.TABLE_STATUS.SUCCESS.equals(status)
                || Constants.TABLE_STATUS.INACTIVE.equals(status);
    }

    private VaccineRecord copyRecordForCoveredDisease(
            VaccineRecord record,
            ChildVaccineDisease disease,
            List<ChildDiseaseDoseSchedule> targetSchedules,
            List<ChildDiseaseDoseSchedule> sourceSchedules,
            LocalDate dateOfBirth
    ) {
        return VaccineRecord.builder()
                .id(record.getId())
                .profileId(record.getProfileId())
                .disease(disease)
                .doseOrder(resolveCoveredDoseOrder(record, targetSchedules, sourceSchedules, dateOfBirth))
                .source(record.getSource())
                .vaccine(record.getVaccine())
                .injectionDate(record.getInjectionDate())
                .actualInjectionDate(record.getActualInjectionDate())
                .price(record.getPrice())
                .note(record.getNote())
                .createdAt(record.getCreatedAt())
                .createdBy(record.getCreatedBy())
                .updatedAt(record.getUpdatedAt())
                .updatedBy(record.getUpdatedBy())
                .status(record.getStatus())
                .build();
    }

    private Integer resolveCoveredDoseOrder(
            VaccineRecord record,
            List<ChildDiseaseDoseSchedule> targetSchedules,
            List<ChildDiseaseDoseSchedule> sourceSchedules,
            LocalDate dateOfBirth
    ) {
        Integer sourceRecommendedAgeMonths = sourceSchedules.stream()
                .filter(schedule -> Objects.equals(schedule.getDoseOrder(), record.getDoseOrder()))
                .map(ChildDiseaseDoseSchedule::getRecommendedAgeMonths)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (sourceRecommendedAgeMonths != null) {
            Integer matchedDoseOrder = targetSchedules.stream()
                    .filter(schedule -> Objects.equals(schedule.getRecommendedAgeMonths(), sourceRecommendedAgeMonths))
                    .map(ChildDiseaseDoseSchedule::getDoseOrder)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (matchedDoseOrder != null) {
                return matchedDoseOrder;
            }
        }

        if (record.getInjectionDate() == null || dateOfBirth == null) {
            return null;
        }

        return targetSchedules.stream()
                .filter(schedule -> schedule.getRecommendedAgeMonths() != null)
                .filter(schedule -> record.getInjectionDate().equals(dateOfBirth.plusMonths(schedule.getRecommendedAgeMonths())))
                .map(ChildDiseaseDoseSchedule::getDoseOrder)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean isOverdue(Long status, LocalDate injectionDate) {
        return Constants.TABLE_STATUS.PENDING.equals(status)
                && injectionDate != null
                && injectionDate.isBefore(LocalDate.now());
    }

    private VaccineRecord preferVisibleRecord(VaccineRecord first, VaccineRecord second) {
        if (Constants.TABLE_STATUS.CANCELED.equals(first.getStatus()) && !Constants.TABLE_STATUS.CANCELED.equals(second.getStatus())) {
            return second;
        }
        return first;
    }

    private List<String> buildRuleWarnings(Long rule) {
        if (VaccineRuleConstants.INTERCHANGE_RULE.ALLOW_INTERCHANGE.equals(rule)) {
            return List.of();
        }
        if (VaccineRuleConstants.INTERCHANGE_RULE.MIXED_SERIES_USE_MAX_DOSE.equals(rule)) {
            return List.of("Nếu trộn sản phẩm, hệ thống cần dùng phác đồ có số liều cao hơn.");
        }
        if (VaccineRuleConstants.INTERCHANGE_RULE.NOT_INTERCHANGEABLE.equals(rule)) {
            return List.of("Không tự ý hoán đổi sản phẩm trong nhóm này.");
        }
        if (VaccineRuleConstants.INTERCHANGE_RULE.SPECIAL_RESTART_REQUIRED.equals(rule)) {
            return List.of("Cần bác sĩ xác nhận và có thể phải tính lại phác đồ.");
        }
        if (VaccineRuleConstants.INTERCHANGE_RULE.ANNUAL_SINGLE_DOSE.equals(rule)) {
            return List.of("Theo dõi lịch tiêm nhắc hằng năm.");
        }
        return List.of("Nên hoàn thành bằng cùng sản phẩm nếu có thể.");
    }

    private void validateProfileOwnership(Long profileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized");
        }

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (profile.getUser() == null || !profile.getUser().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
    }

    private VaccineResponse mapVaccine(Vaccine vaccine) {
        return VaccineResponse.builder()
                .id(vaccine.getId())
                .name(vaccine.getName())
                .manufacturer(vaccine.getManufacturer())
                .origin(vaccine.getOrigin())
                .description(vaccine.getDescription())
                .status(vaccine.getStatus())
                .build();
    }

    private ChildVaccineDiseaseResponse mapChildDisease(ChildVaccineDisease disease) {
        return ChildVaccineDiseaseResponse.builder()
                .id(disease.getId())
                .code(disease.getCode())
                .name(disease.getName())
                .description(disease.getDescription())
                .required(disease.getRequired())
                .displayOrder(disease.getDisplayOrder())
                .status(disease.getStatus())
                .build();
    }

    private VaccineDiseaseCoverageResponse mapCoverage(VaccineDiseaseCoverage coverage) {
        Vaccine vaccine = coverage.getVaccine();
        ChildVaccineDisease disease = coverage.getDisease();
        return VaccineDiseaseCoverageResponse.builder()
                .id(coverage.getId())
                .vaccineId(vaccine != null ? vaccine.getId() : null)
                .vaccineName(vaccine != null ? vaccine.getName() : null)
                .manufacturer(vaccine != null ? vaccine.getManufacturer() : null)
                .diseaseId(disease != null ? disease.getId() : null)
                .diseaseCode(disease != null ? disease.getCode() : null)
                .diseaseName(disease != null ? disease.getName() : null)
                .productFamilyCode(coverage.getProductFamilyCode())
                .interchangeRule(coverage.getInterchangeRule())
                .interchangeRuleCode(VaccineRuleConstants.INTERCHANGE_RULE.codeOf(coverage.getInterchangeRule()))
                .status(coverage.getStatus())
                .build();
    }

    private ChildDiseaseDoseScheduleResponse mapChildDoseSchedule(ChildDiseaseDoseSchedule schedule) {
        ChildVaccineDisease disease = schedule.getDisease();
        return ChildDiseaseDoseScheduleResponse.builder()
                .id(schedule.getId())
                .diseaseId(disease != null ? disease.getId() : null)
                .diseaseCode(disease != null ? disease.getCode() : null)
                .diseaseName(disease != null ? disease.getName() : null)
                .doseOrder(schedule.getDoseOrder())
                .recommendedAgeMonths(schedule.getRecommendedAgeMonths())
                .intervalDays(schedule.getIntervalDays())
                .doseLabel(schedule.getDoseLabel())
                .note(schedule.getNote())
                .status(schedule.getStatus())
                .build();
    }

    private ChildDiseaseDoseVaccineOptionResponse mapChildDoseVaccineOption(ChildDiseaseDoseVaccineOption option) {
        ChildVaccineDisease disease = option.getDisease();
        Vaccine vaccine = option.getVaccine();
        return ChildDiseaseDoseVaccineOptionResponse.builder()
                .id(option.getId())
                .diseaseId(disease != null ? disease.getId() : null)
                .doseOrder(option.getDoseOrder())
                .vaccineId(vaccine != null ? vaccine.getId() : null)
                .vaccineName(vaccine != null ? vaccine.getName() : null)
                .manufacturer(vaccine != null ? vaccine.getManufacturer() : null)
                .origin(vaccine != null ? vaccine.getOrigin() : null)
                .description(vaccine != null ? vaccine.getDescription() : null)
                .preferred(option.getPreferred())
                .displayOrder(option.getDisplayOrder())
                .note(option.getNote())
                .status(option.getStatus())
                .build();
    }
}
