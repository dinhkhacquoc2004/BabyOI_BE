package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.BabyRoutineEntryCreateRequest;
import com.example.babyoi_be.domain.dto.request.BabyRoutineEntryUpdateRequest;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineAiAnalysisResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineDayResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineEntryResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineHistoryResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineNightEventPredictionResponse;
import com.example.babyoi_be.domain.dto.respone.BabyRoutineSleepPredictionResponse;
import com.example.babyoi_be.domain.entity.BabyRoutineEntry;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.repository.BabyRoutineEntryRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.BabyRoutineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BabyRoutineServiceImpl implements BabyRoutineService {

    private static final String DISCLAIMER = "Gợi ý chỉ mang tính tham khảo theo độ tuổi và nhật ký của bé, không thay thế tư vấn của bác sĩ nhi khoa.";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BabyRoutineEntryRepository babyRoutineEntryRepository;
    private final ProfileRepository profileRepository;

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${app.ai.gemini.text-model:gemini-3.5-flash,gemini-2.5-flash,gemini-2.5-flash-lite}")
    private String textModels;

    @Value("${app.ai.gemini.timeout-seconds:20}")
    private Long timeoutSeconds;

    @Override
    @Transactional
    public BabyRoutineDayResponse getDailyRoutine(Long profileId, LocalDate routineDate) {
        Profile profile = resolveChildProfile(profileId, routineDate);
        List<BabyRoutineEntry> entries = getOrCreateRoutineEntries(profile, routineDate);
        return buildDayResponse(profile, routineDate, entries, buildRuleBasedAnalysis(profile, routineDate, entries, null));
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void autoCompleteOverdueRoutineEntries() {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        LocalDate today = now.toLocalDate();
        LocalDateTime overdueCutoff = now.minusHours(1);

        List<BabyRoutineEntry> entries = new ArrayList<>();
        entries.addAll(babyRoutineEntryRepository
                .findByRoutineDateBeforeAndStatusAndCompletedFalseOrderByRoutineDateAscPlannedTimeAscIdAsc(
                        today,
                        Constants.TABLE_STATUS.ACTIVE
                ));
        if (overdueCutoff.toLocalDate().isEqual(today)) {
            entries.addAll(babyRoutineEntryRepository
                    .findByRoutineDateAndStatusAndCompletedFalseAndPlannedTimeLessThanEqualOrderByPlannedTimeAscIdAsc(
                            today,
                            Constants.TABLE_STATUS.ACTIVE,
                            overdueCutoff.toLocalTime()
                    ));
        }

        autoCompleteRoutineEntries(entries, now);
    }

    @Override
    @Transactional
    public BabyRoutineHistoryResponse getRoutineHistory(Long profileId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be before or equal to toDate");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > 370) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xem lịch sinh hoạt tối đa 1 năm mỗi lần");
        }

        Profile profile = resolveChildProfileForRoutine(profileId);
        LocalDate supportedFrom = resolveRoutineStartDate(profile);
        LocalDate supportedTo = profile.getDateOfBirth().plusMonths(25).minusDays(1);
        LocalDate effectiveFrom = fromDate.isBefore(supportedFrom) ? supportedFrom : fromDate;
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate requestedTo = toDate.isAfter(today) ? today : toDate;
        LocalDate effectiveTo = requestedTo.isAfter(supportedTo) ? supportedTo : requestedTo;

        List<BabyRoutineDayResponse> days = new ArrayList<>();
        if (!effectiveFrom.isAfter(effectiveTo)) {
            LocalDate currentDate = effectiveFrom;
            while (!currentDate.isAfter(effectiveTo)) {
                List<BabyRoutineEntry> entries = getOrCreateRoutineEntries(profile, currentDate);
                days.add(buildDayResponse(profile, currentDate, entries, null));
                currentDate = currentDate.plusDays(1);
            }
        }

        return BabyRoutineHistoryResponse.builder()
                .profileId(profile.getId())
                .profileName(profile.getName())
                .fromDate(fromDate.toString())
                .toDate(toDate.toString())
                .totalDays(days.size())
                .days(days)
                .build();
    }

    @Override
    @Transactional
    public BabyRoutineDayResponse applyNextDayRoutineAdjustment(Long profileId, LocalDate routineDate) {
        if (routineDate.isAfter(LocalDate.now(APP_ZONE))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể cập nhật dự đoán từ một ngày trong tương lai");
        }

        Profile profile = resolveChildProfile(profileId, routineDate);
        List<BabyRoutineEntry> todayEntries = getOrCreateRoutineEntries(profile, routineDate);
        validateRoutineDayCompleted(todayEntries);

        LocalDate nextDate = routineDate.plusDays(1);
        if (resolveAgeMonths(profile, nextDate) > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Routine hiện chỉ hỗ trợ bé từ 0 đến 24 tháng");
        }

        int shiftMinutes = calculateRoutineShiftMinutes(todayEntries);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<BabyRoutineEntry> tomorrowEntries = getOrCreateRoutineEntries(profile, nextDate);
        List<RoutineSpec> tomorrowSpecs = routineSpecs(resolveAgeMonths(profile, nextDate));

        for (int index = 0; index < tomorrowEntries.size(); index++) {
            BabyRoutineEntry entry = tomorrowEntries.get(index);
            if (Boolean.TRUE.equals(entry.getCompleted())) {
                continue;
            }
            if (index < tomorrowSpecs.size()) {
                entry.setPlannedTime(LocalTime.parse(tomorrowSpecs.get(index).time()).plusMinutes(shiftMinutes));
            } else {
                entry.setPlannedTime(entry.getPlannedTime().plusMinutes(shiftMinutes));
            }
            entry.setNote("Đậu sức khỏe đã cập nhật từ lịch hoàn thành ngày " + routineDate);
            entry.setSource("DAU_HEALTH_PREDICTION");
            entry.setUpdatedAt(now);
        }

        List<BabyRoutineEntry> savedEntries = babyRoutineEntryRepository.saveAll(tomorrowEntries);
        String summary = "Lịch ngày mai đã được Đậu sức khỏe điều chỉnh "
                + formatSignedMinutes(shiftMinutes)
                + " dựa trên giờ sinh hoạt thực tế của ngày " + routineDate + ".";
        return buildDayResponse(profile, nextDate, savedEntries, buildRuleBasedAnalysis(profile, nextDate, savedEntries, summary));
    }

    @Override
    @Transactional
    public BabyRoutineEntryResponse createRoutineEntry(Long profileId, BabyRoutineEntryCreateRequest request) {
        LocalDate routineDate = parseRoutineDate(request.getRoutineDate());
        Profile profile = resolveChildProfile(profileId, routineDate);
        List<BabyRoutineEntry> entries = getOrCreateRoutineEntries(profile, routineDate);
        LocalTime plannedTime = parseRequiredTime(request.getPlannedTime(), "plannedTime");
        validateNewRoutineTime(entries, plannedTime);

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        BabyRoutineEntry entry = BabyRoutineEntry.builder()
                .profile(profile)
                .routineDate(routineDate)
                .type(compactText(defaultText(request.getType(), "CUSTOM"), 30))
                .plannedTime(plannedTime)
                .activity(compactText(request.getActivity(), 255))
                .note(compactText(request.getNote(), 1000))
                .icon(compactText(defaultText(request.getIcon(), "calendar"), 40))
                .color(compactText(defaultText(request.getColor(), "#FF8FAB"), 20))
                .completed(false)
                .source("CUSTOM")
                .status(Constants.TABLE_STATUS.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return mapEntry(babyRoutineEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public BabyRoutineEntryResponse updateRoutineEntry(Long entryId, BabyRoutineEntryUpdateRequest request) {
        BabyRoutineEntry entry = babyRoutineEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine entry not found"));
        validateEntryActive(entry);
        validateCurrentUser(entry.getProfile().getUser().getId());

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        boolean wasCompleted = Boolean.TRUE.equals(entry.getCompleted());
        if (wasCompleted && !Boolean.TRUE.equals(request.getCompleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mục đã hoàn thành chỉ có thể sửa giờ, không thể bỏ tích");
        }
        if (!wasCompleted && Boolean.TRUE.equals(request.getCompleted())) {
            validateCanCompleteRoutineEntry(entry);
        }

        LocalTime actualTime = Boolean.TRUE.equals(request.getCompleted())
                ? parseOptionalTime(request.getActualTime(), LocalTime.now(APP_ZONE))
                : null;
        if (actualTime != null) {
            validateActualTimeAfterPrevious(entry, actualTime);
        }
        entry.setCompleted(request.getCompleted());
        entry.setActualTime(actualTime);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            entry.setNote(request.getNote().trim());
        } else if (Boolean.TRUE.equals(request.getCompleted())) {
            entry.setNote("Hoàn thành lúc " + formatTime(actualTime));
        } else {
            entry.setNote(null);
        }
        entry.setUpdatedAt(now);
        BabyRoutineEntry savedEntry = babyRoutineEntryRepository.save(entry);
        syncNextDayAfterNightSleepUpdate(savedEntry);
        return mapEntry(savedEntry);
    }

    @Override
    @Transactional
    public void deleteRoutineEntry(Long entryId) {
        BabyRoutineEntry entry = babyRoutineEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine entry not found"));
        validateCurrentUser(entry.getProfile().getUser().getId());
        if (Constants.TABLE_STATUS.DELETED.equals(entry.getStatus())) {
            return;
        }
        validateEntryActive(entry);

        entry.setStatus(Constants.TABLE_STATUS.DELETED);
        entry.setUpdatedAt(LocalDateTime.now(APP_ZONE));
        babyRoutineEntryRepository.save(entry);
    }

    @Override
    @Transactional
    public BabyRoutineDayResponse updateTimelineFromEntry(Long entryId) {
        BabyRoutineEntry anchorEntry = babyRoutineEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine entry not found"));
        validateEntryActive(anchorEntry);
        validateCurrentUser(anchorEntry.getProfile().getUser().getId());
        if (!Boolean.TRUE.equals(anchorEntry.getCompleted()) || anchorEntry.getActualTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể update timeline từ mục đã hoàn thành");
        }

        Profile profile = anchorEntry.getProfile();
        LocalDate routineDate = anchorEntry.getRoutineDate();
        List<BabyRoutineEntry> entries = getOrCreateRoutineEntries(profile, routineDate);
        int anchorIndex = findEntryIndex(entries, anchorEntry.getId());
        if (anchorIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine entry not found");
        }

        List<RoutineSpec> specs = routineSpecs(resolveAgeMonths(profile, routineDate));
        LocalTime anchorBaselineTime = anchorIndex < specs.size()
                ? LocalTime.parse(specs.get(anchorIndex).time())
                : entries.get(anchorIndex).getPlannedTime();
        long deltaMinutes = Duration.between(anchorBaselineTime, anchorEntry.getActualTime()).toMinutes();
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        for (int index = anchorIndex + 1; index < entries.size(); index++) {
            BabyRoutineEntry entry = entries.get(index);
            if (Boolean.TRUE.equals(entry.getCompleted())) {
                continue;
            }
            LocalTime baselineTime = index < specs.size()
                    ? LocalTime.parse(specs.get(index).time())
                    : entry.getPlannedTime();
            entry.setPlannedTime(baselineTime.plusMinutes(deltaMinutes));
            entry.setNote("Đậu sức khỏe đã update timeline theo " + anchorEntry.getActivity());
            entry.setSource("TIMELINE_UPDATE");
            entry.setUpdatedAt(now);
        }

        List<BabyRoutineEntry> savedEntries = babyRoutineEntryRepository.saveAll(entries);
        String summary = "Timeline đã được update " + formatSignedMinutes((int) deltaMinutes)
                + " dựa trên giờ hoàn thành của mục \"" + anchorEntry.getActivity() + "\".";
        return buildDayResponse(profile, routineDate, savedEntries, buildRuleBasedAnalysis(profile, routineDate, savedEntries, summary));
    }

    @Override
    @Transactional
    public BabyRoutineAiAnalysisResponse analyzeRoutine(Long profileId, LocalDate routineDate) {
        Profile profile = resolveChildProfile(profileId, routineDate);
        List<BabyRoutineEntry> entries = getOrCreateRoutineEntries(profile, routineDate);
        BabyRoutineAiAnalysisResponse fallback = buildRuleBasedAnalysis(profile, routineDate, entries, null);

        String geminiSummary = callGeminiForRoutine(profile, routineDate, entries);
        if (geminiSummary == null || geminiSummary.isBlank()) {
            return fallback;
        }

        return BabyRoutineAiAnalysisResponse.builder()
                .title("Đậu sức khỏe nhận xét lịch sinh hoạt")
                .summary(geminiSummary)
                .recommendedTomorrowWakeTime(fallback.getRecommendedTomorrowWakeTime())
                .highlights(fallback.getHighlights())
                .warnings(fallback.getWarnings())
                .suggestions(fallback.getSuggestions())
                .disclaimer(DISCLAIMER)
                .build();
    }

    private List<BabyRoutineEntry> getOrCreateRoutineEntries(Profile profile, LocalDate routineDate) {
        List<BabyRoutineEntry> existingEntries = babyRoutineEntryRepository
                .findByProfile_IdAndRoutineDateAndStatusOrderByPlannedTimeAscIdAsc(
                        profile.getId(),
                        routineDate,
                        Constants.TABLE_STATUS.ACTIVE
                );
        if (!existingEntries.isEmpty()) {
            List<BabyRoutineEntry> syncedEntries = syncGuidelineRoutineEntries(profile, routineDate, existingEntries);
            syncedEntries = syncPredictedNightRoutineEntries(profile, routineDate, syncedEntries);
            return autoCompleteRoutineEntries(syncedEntries, LocalDateTime.now(APP_ZONE));
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<BabyRoutineEntry> generatedEntries = routineSpecs(resolveAgeMonths(profile, routineDate))
                .stream()
                .map(spec -> BabyRoutineEntry.builder()
                        .profile(profile)
                        .routineDate(routineDate)
                        .type(spec.type())
                        .plannedTime(LocalTime.parse(spec.time()))
                        .activity(spec.activity())
                        .note(spec.note())
                        .icon(spec.icon())
                        .color(spec.color())
                        .completed(false)
                        .source("AGE_GUIDELINE")
                        .status(Constants.TABLE_STATUS.ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
        List<BabyRoutineEntry> savedEntries = babyRoutineEntryRepository.saveAll(generatedEntries);
        List<BabyRoutineEntry> syncedEntries = syncPredictedNightRoutineEntries(profile, routineDate, savedEntries);
        return autoCompleteRoutineEntries(syncedEntries, LocalDateTime.now(APP_ZONE));
    }

    private List<BabyRoutineEntry> syncGuidelineRoutineEntries(
            Profile profile,
            LocalDate routineDate,
            List<BabyRoutineEntry> activeEntries
    ) {
        List<RoutineSpec> specs = routineSpecs(resolveAgeMonths(profile, routineDate));
        boolean hasMissingSpec = specs.stream().anyMatch(spec -> !hasRoutineSpec(activeEntries, spec));
        if (!hasMissingSpec) {
            return activeEntries;
        }

        List<BabyRoutineEntry> allEntries = babyRoutineEntryRepository
                .findByProfile_IdAndRoutineDateOrderByPlannedTimeAscIdAsc(profile.getId(), routineDate);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<BabyRoutineEntry> missingEntries = specs.stream()
                .filter(spec -> !hasRoutineSpec(allEntries, spec))
                .map(spec -> BabyRoutineEntry.builder()
                        .profile(profile)
                        .routineDate(routineDate)
                        .type(spec.type())
                        .plannedTime(LocalTime.parse(spec.time()))
                        .activity(spec.activity())
                        .note(spec.note())
                        .icon(spec.icon())
                        .color(spec.color())
                        .completed(false)
                        .source("AGE_GUIDELINE")
                        .status(Constants.TABLE_STATUS.ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
        if (missingEntries.isEmpty()) {
            return activeEntries;
        }

        List<BabyRoutineEntry> syncedEntries = new ArrayList<>(activeEntries);
        syncedEntries.addAll(babyRoutineEntryRepository.saveAll(missingEntries));
        syncedEntries.sort(Comparator.comparing(BabyRoutineEntry::getPlannedTime).thenComparing(BabyRoutineEntry::getId));
        return syncedEntries;
    }

    private boolean hasRoutineSpec(List<BabyRoutineEntry> entries, RoutineSpec spec) {
        LocalTime plannedTime = LocalTime.parse(spec.time());
        return entries.stream().anyMatch(entry ->
                spec.type().equals(entry.getType())
                        && plannedTime.equals(entry.getPlannedTime())
                        && spec.activity().equals(entry.getActivity())
        );
    }

    private List<BabyRoutineEntry> syncPredictedNightRoutineEntries(
            Profile profile,
            LocalDate routineDate,
            List<BabyRoutineEntry> activeEntries
    ) {
        List<BabyRoutineEntry> usableEntries = removeUncompletedStaticNightGuidelines(activeEntries);
        List<PredictedRoutineSpec> predictedSpecs = predictedNightRoutineSpecs(profile, routineDate, usableEntries);
        usableEntries = removeStalePredictedNightEntries(routineDate, usableEntries, predictedSpecs);
        if (predictedSpecs.isEmpty()) {
            return usableEntries;
        }

        List<BabyRoutineEntry> allEntries = babyRoutineEntryRepository
                .findByProfile_IdAndRoutineDateOrderByPlannedTimeAscIdAsc(profile.getId(), routineDate);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<BabyRoutineEntry> changedEntries = new ArrayList<>();
        List<BabyRoutineEntry> newEntries = new ArrayList<>();

        for (PredictedRoutineSpec spec : predictedSpecs) {
            BabyRoutineEntry existingActive = findPredictedNightEntry(usableEntries, spec);
            if (existingActive != null) {
                if (syncPredictedNightEntry(existingActive, spec, now)) {
                    changedEntries.add(existingActive);
                }
                continue;
            }
            if (findPredictedNightEntry(allEntries, spec) != null) {
                continue;
            }
            newEntries.add(BabyRoutineEntry.builder()
                    .profile(profile)
                    .routineDate(routineDate)
                    .type(spec.type())
                    .plannedTime(spec.dateTime().toLocalTime())
                    .activity(spec.activity())
                    .note(spec.note())
                    .icon(spec.icon())
                    .color(spec.color())
                    .completed(false)
                    .source("NIGHT_PREDICTION")
                    .status(Constants.TABLE_STATUS.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        List<BabyRoutineEntry> syncedEntries = new ArrayList<>(usableEntries);
        if (!changedEntries.isEmpty()) {
            babyRoutineEntryRepository.saveAll(changedEntries);
        }
        if (!newEntries.isEmpty()) {
            syncedEntries.addAll(babyRoutineEntryRepository.saveAll(newEntries));
        }
        syncedEntries.sort(Comparator.comparing(BabyRoutineEntry::getPlannedTime).thenComparing(BabyRoutineEntry::getId));
        return syncedEntries;
    }

    private List<BabyRoutineEntry> removeUncompletedStaticNightGuidelines(List<BabyRoutineEntry> activeEntries) {
        List<BabyRoutineEntry> staleEntries = activeEntries.stream()
                .filter(this::isUncompletedStaticNightGuideline)
                .toList();
        if (staleEntries.isEmpty()) {
            return activeEntries;
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        staleEntries.forEach(entry -> {
            entry.setStatus(Constants.TABLE_STATUS.DELETED);
            entry.setUpdatedAt(now);
        });
        babyRoutineEntryRepository.saveAll(staleEntries);
        return activeEntries.stream()
                .filter(entry -> !staleEntries.contains(entry))
                .toList();
    }

    private List<BabyRoutineEntry> removeStalePredictedNightEntries(
            LocalDate routineDate,
            List<BabyRoutineEntry> activeEntries,
            List<PredictedRoutineSpec> predictedSpecs
    ) {
        LocalDate previousDate = routineDate.minusDays(1);
        List<BabyRoutineEntry> staleEntries = activeEntries.stream()
                .filter(entry -> "NIGHT_PREDICTION".equals(entry.getSource()))
                .filter(entry -> isPredictedFromSleepDate(entry, previousDate) || isPredictedFromSleepDate(entry, routineDate))
                .filter(entry -> predictedSpecs.stream().noneMatch(spec -> isSamePredictedNightEntry(entry, spec)))
                .toList();
        if (staleEntries.isEmpty()) {
            return activeEntries;
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        staleEntries.forEach(entry -> {
            entry.setStatus(Constants.TABLE_STATUS.DELETED);
            entry.setUpdatedAt(now);
        });
        babyRoutineEntryRepository.saveAll(staleEntries);
        return activeEntries.stream()
                .filter(entry -> !staleEntries.contains(entry))
                .toList();
    }

    private boolean isPredictedFromSleepDate(BabyRoutineEntry entry, LocalDate sleepDate) {
        return entry.getNote() != null && entry.getNote().contains("ngày " + sleepDate);
    }

    private boolean isUncompletedStaticNightGuideline(BabyRoutineEntry entry) {
        if (!"AGE_GUIDELINE".equals(entry.getSource()) || Boolean.TRUE.equals(entry.getCompleted())) {
            return false;
        }
        return "NIGHT_FEED".equals(entry.getType())
                || "NIGHT_WAKE".equals(entry.getType())
                || ("SLEEP".equals(entry.getType()) && entry.getActivity() != null && entry.getActivity().startsWith("Ngủ tiếp"));
    }

    private List<PredictedRoutineSpec> predictedNightRoutineSpecs(
            Profile profile,
            LocalDate routineDate,
            List<BabyRoutineEntry> currentDateEntries
    ) {
        List<PredictedRoutineSpec> specs = new ArrayList<>();
        LocalDate previousDate = routineDate.minusDays(1);
        List<BabyRoutineEntry> previousEntries = babyRoutineEntryRepository
                .findByProfile_IdAndRoutineDateAndStatusOrderByPlannedTimeAscIdAsc(
                        profile.getId(),
                        previousDate,
                        Constants.TABLE_STATUS.ACTIVE
                );
        addPredictedNightRoutineSpecs(profile, previousDate, routineDate, previousEntries, specs);
        addPredictedNightRoutineSpecs(profile, routineDate, routineDate, currentDateEntries, specs);
        specs.sort(Comparator.comparing(PredictedRoutineSpec::dateTime));
        return specs;
    }

    private void addPredictedNightRoutineSpecs(
            Profile profile,
            LocalDate sleepDate,
            LocalDate targetDate,
            List<BabyRoutineEntry> entries,
            List<PredictedRoutineSpec> specs
    ) {
        BabyRoutineEntry sleepEntry = entries.stream()
                .filter(entry -> "SLEEP".equals(entry.getType()))
                .filter(entry -> !"NIGHT_PREDICTION".equals(entry.getSource()))
                .max(Comparator.comparing(entry -> entry.getActualTime() != null ? entry.getActualTime() : entry.getPlannedTime()))
                .orElse(null);
        if (sleepEntry == null) {
            return;
        }

        LocalTime sleepStartTime = sleepEntry.getActualTime() != null ? sleepEntry.getActualTime() : sleepEntry.getPlannedTime();
        LocalDateTime sleepStartDateTime = LocalDateTime.of(sleepDate, sleepStartTime);
        int ageMonths = resolveAgeMonths(profile, sleepDate);
        for (PredictedNightEvent event : predictedNightEvents(ageMonths, sleepStartDateTime)) {
            if (event.likelyDateTime().toLocalDate().isEqual(targetDate)) {
                specs.add(predictedSpec(
                        event.type(),
                        event.likelyDateTime(),
                        event.label(),
                        event.note(),
                        event.type().contains("FEED") ? "water" : "happy",
                        event.type().contains("FEED") ? "#A7E4F5" : "#FFD166",
                        sleepDate
                ));
            }
            LocalDateTime sleepBackDateTime = event.likelyDateTime().plusMinutes(20);
            if (event.addSleepBack() && sleepBackDateTime.toLocalDate().isEqual(targetDate)) {
                specs.add(predictedSpec(
                        "SLEEP",
                        sleepBackDateTime,
                        "Ngủ tiếp sau " + event.label().toLowerCase(Locale.ROOT),
                        "Tích khi bé đã được hỗ trợ và quay lại giấc ngủ.",
                        "moon",
                        "#5C7A8A",
                        sleepDate
                ));
            }
        }
    }

    private List<PredictedNightEvent> predictedNightEvents(int ageMonths, LocalDateTime sleepStartDateTime) {
        if (ageMonths < 3) {
            return List.of(
                    predictedEvent("NIGHT_FEED", "Cữ đêm 1", sleepStartDateTime, 180,
                            "Bé 0-2 tháng thường thức sau 2-4 giờ để bú hoặc được vỗ về.", true),
                    predictedEvent("NIGHT_FEED", "Cữ đêm 2", sleepStartDateTime, 390,
                            "Cữ đêm tiếp theo có thể xuất hiện nếu bé chưa ngủ đủ hoặc đói.", true),
                    predictedEvent("EARLY_MORNING_FEED", "Cữ gần sáng", sleepStartDateTime, 540,
                            "Gần sáng bé có thể bú thêm rồi ngủ tiếp hoặc bắt đầu ngày mới.", true)
            );
        }
        if (ageMonths < 6) {
            return List.of(
                    predictedEvent("NIGHT_FEED", "Cữ đêm có thể xảy ra", sleepStartDateTime, 360,
                            "Bé 3-5 tháng có thể ngủ một mạch khoảng 6 tiếng, nhưng thức 1-2 lần ban đêm vẫn bình thường.", true),
                    predictedEvent("EARLY_MORNING_WAKE", "Dậy sáng sớm", sleepStartDateTime, 600,
                            "Nếu đêm ổn, bé có thể dậy vào khoảng sáng sớm sau giấc ngủ dài.", false)
            );
        }
        if (ageMonths < 9) {
            return List.of(
                    predictedEvent("POSSIBLE_WAKE", "Có thể dậy giữa đêm", sleepStartDateTime, 480,
                            "Bé có thể thức ngắn khi đói, mọc răng hoặc cần trấn an.", true),
                    predictedEvent("EARLY_MORNING_WAKE", "Dậy buổi sáng", sleepStartDateTime, 600,
                            "Sau giấc đêm dài, bé thường dậy vào sáng sớm.", false)
            );
        }
        if (ageMonths < 13) {
            return List.of(
                    predictedEvent("SLEEP_REGRESSION_WAKE", "Có thể thức ngắn", sleepStartDateTime, 420,
                            "Phát triển kỹ năng, mọc răng hoặc bám mẹ có thể làm bé thức ngắn.", true),
                    predictedEvent("MORNING_WAKE", "Dậy buổi sáng", sleepStartDateTime, 660,
                            "Nếu giấc đêm ổn, đây là khoảng bé có thể dậy để bắt đầu ngày mới.", false)
            );
        }
        return List.of(
                predictedEvent("MORNING_WAKE", "Dậy buổi sáng", sleepStartDateTime, 660,
                        "Mốc này giúp mẹ dự đoán giờ dậy sáng.", false)
        );
    }

    private PredictedNightEvent predictedEvent(
            String type,
            String label,
            LocalDateTime sleepStartDateTime,
            int likelyOffsetMinutes,
            String note,
            boolean addSleepBack
    ) {
        return new PredictedNightEvent(
                type,
                label,
                sleepStartDateTime.plusMinutes(likelyOffsetMinutes),
                note,
                addSleepBack
        );
    }

    private PredictedRoutineSpec predictedSpec(
            String type,
            LocalDateTime dateTime,
            String activity,
            String note,
            String icon,
            String color,
            LocalDate sleepDate
    ) {
        return new PredictedRoutineSpec(
                type,
                dateTime,
                activity,
                note + " Dự đoán từ giấc ngủ cuối ngày " + sleepDate + ".",
                icon,
                color,
                sleepDate.toString()
        );
    }

    private BabyRoutineEntry findPredictedNightEntry(List<BabyRoutineEntry> entries, PredictedRoutineSpec spec) {
        return entries.stream()
                .filter(entry -> "NIGHT_PREDICTION".equals(entry.getSource()))
                .filter(entry -> isSamePredictedNightEntry(entry, spec))
                .findFirst()
                .orElse(null);
    }

    private boolean isSamePredictedNightEntry(BabyRoutineEntry entry, PredictedRoutineSpec spec) {
        return spec.type().equals(entry.getType())
                && spec.activity().equals(entry.getActivity())
                && entry.getNote() != null
                && entry.getNote().contains("ngày " + spec.sourceDate());
    }

    private boolean syncPredictedNightEntry(BabyRoutineEntry entry, PredictedRoutineSpec spec, LocalDateTime now) {
        boolean changed = false;
        LocalTime plannedTime = spec.dateTime().toLocalTime();
        if (!plannedTime.equals(entry.getPlannedTime())) {
            entry.setPlannedTime(plannedTime);
            changed = true;
        }
        if (!spec.note().equals(entry.getNote())) {
            entry.setNote(spec.note());
            changed = true;
        }
        if (!spec.icon().equals(entry.getIcon())) {
            entry.setIcon(spec.icon());
            changed = true;
        }
        if (!spec.color().equals(entry.getColor())) {
            entry.setColor(spec.color());
            changed = true;
        }
        if (changed) {
            entry.setUpdatedAt(now);
        }
        return changed;
    }

    private void syncNextDayAfterNightSleepUpdate(BabyRoutineEntry entry) {
        if (!"SLEEP".equals(entry.getType()) || "NIGHT_PREDICTION".equals(entry.getSource())) {
            return;
        }
        LocalDate nextDate = entry.getRoutineDate().plusDays(1);
        if (resolveAgeMonths(entry.getProfile(), nextDate) > 24) {
            return;
        }
        getOrCreateRoutineEntries(entry.getProfile(), nextDate);
    }

    private List<RoutineSpec> routineSpecs(int ageMonths) {
        if (ageMonths < 3) {
            return List.of(
                    spec("WAKE", "06:30", "Bé thức dậy", "Trẻ sơ sinh giai đoạn đầu ngủ rất nhiều và thức dậy chủ yếu để bú.", "sunny", "#FFB6C1"),
                    spec("MILK", "06:45", "Cữ sữa sáng", "Cho bú theo nhu cầu, ưu tiên đủ năng lượng trong những tuần đầu.", "water", "#A7E4F5"),
                    spec("NAP", "07:30", "Giấc ngủ sáng", "Giấc ngủ thường ngắn, chia đều ngày và đêm.", "bed", "#74C0FC"),
                    spec("MILK", "09:30", "Cữ sữa giữa sáng", "Bé có thể thức vì đói sau mỗi cụm ngủ ngắn.", "water", "#A7E4F5"),
                    spec("NAP", "10:00", "Giấc ngủ gần trưa", "Giữ môi trường ngủ an toàn: nằm ngửa, mặt phẳng thoáng.", "bed", "#74C0FC"),
                    spec("MILK", "12:00", "Cữ sữa trưa", "Tiếp tục bú đầy đủ theo nhu cầu của bé.", "water", "#A7E4F5"),
                    spec("NAP", "12:45", "Giấc ngủ trưa", "Ban ngày có thể ngủ tổng khoảng 8 tiếng.", "bed", "#74C0FC"),
                    spec("MILK", "15:00", "Cữ sữa chiều", "Theo dõi tã ướt, cân nặng và dấu hiệu đói/no.", "water", "#A7E4F5"),
                    spec("NAP", "15:45", "Giấc ngủ chiều", "Không ép lịch cứng, quan sát tín hiệu buồn ngủ.", "bed", "#74C0FC"),
                    spec("BATH", "18:00", "Tắm và thư giãn", "Giảm kích thích, ánh sáng dịu trước giấc đêm.", "sparkles", "#FF8FAB"),
                    spec("MILK", "18:30", "Cữ sữa trước ngủ", "Bú đủ trước khi vào giấc đêm, bé vẫn có thể dậy 2-3 lần để bú.", "water", "#A7E4F5"),
                    spec("SLEEP", "19:00", "Ngủ đêm", "Ban đêm có thể ngủ khoảng 9 tiếng nhưng thường thức dậy vì đói.", "moon", "#5C7A8A")
            );
        }
        if (ageMonths < 6) {
            return List.of(
                    spec("WAKE", "07:00", "Bé thức dậy", "Bé 3-5 tháng đã phân biệt ngày đêm rõ hơn.", "sunny", "#FFB6C1"),
                    spec("MILK", "07:30", "Cữ sữa sáng", "Sữa mẹ/sữa công thức vẫn là nguồn dinh dưỡng chính.", "water", "#A7E4F5"),
                    spec("NAP", "09:00", "Giấc ngủ sáng", "Mục tiêu tổng ngủ khoảng 14-16 giờ/ngày.", "bed", "#74C0FC"),
                    spec("PLAY", "10:30", "Tương tác nhẹ", "Trò chuyện, tập lật và chơi trên sàn an toàn.", "happy", "#FFD166"),
                    spec("MILK", "11:00", "Cữ sữa trưa", "Theo dõi tín hiệu đói/no, không ép bé bú hết bình.", "water", "#A7E4F5"),
                    spec("NAP", "12:30", "Giấc ngủ trưa", "Đặt bé xuống cũi/nôi khi lim dim để tập tự ngủ.", "bed", "#74C0FC"),
                    spec("MILK", "15:00", "Cữ sữa chiều", "Một số bé có thể ngủ liền 6 tiếng ban đêm, thức 1-2 lần vẫn bình thường.", "water", "#A7E4F5"),
                    spec("NAP", "16:00", "Giấc ngủ chiều ngắn", "Giấc chiều nên vừa đủ để không quá mệt trước ngủ đêm.", "bed", "#74C0FC"),
                    spec("BATH", "18:30", "Tắm và thư giãn", "Giữ routine tối lặp lại: tắm, massage, ánh sáng dịu.", "sparkles", "#FF8FAB"),
                    spec("SLEEP", "19:30", "Ngủ đêm", "Khuyến khích nhịp ngày đêm ổn định, vẫn đáp ứng khi bé cần.", "moon", "#5C7A8A")
            );
        }
        if (ageMonths < 9) {
            return List.of(
                    spec("WAKE", "07:00", "Bé thức dậy", "Bé 6-8 tháng thường ngủ tổng khoảng 14 giờ/ngày.", "sunny", "#FFB6C1"),
                    spec("MILK", "07:30", "Cữ sữa sáng", "Sữa vẫn là nền chính, ăn dặm chỉ bổ sung.", "water", "#A7E4F5"),
                    spec("NAP", "09:00", "Giấc ngủ 1", "Thường có 2-3 giấc ngủ ngắn, tổng ngủ ngày khoảng 3-4 giờ.", "bed", "#74C0FC"),
                    spec("EAT", "11:00", "Ăn dặm bữa 1", "Ưu tiên thực phẩm mềm, giàu sắt, không nêm muối/đường.", "restaurant", "#FF8FAB"),
                    spec("NAP", "12:30", "Giấc ngủ 2", "Nap đều giúp bé đỡ quá mệt về chiều.", "bed", "#74C0FC"),
                    spec("MILK", "14:30", "Cữ sữa chiều", "Bổ sung sữa theo nhu cầu và khuyến nghị riêng của bé.", "water", "#A7E4F5"),
                    spec("NAP", "15:30", "Giấc ngủ 3 ngắn", "Nếu bé chỉ cần 2 giấc, có thể bỏ giấc này.", "bed", "#74C0FC"),
                    spec("PLAY", "16:30", "Vận động và tương tác", "Tập ngồi/bò theo khả năng, luôn có người lớn bên cạnh.", "happy", "#FFD166"),
                    spec("BATH", "18:30", "Tắm và thư giãn", "Giảm kích thích trước giấc ngủ đêm.", "sparkles", "#FF8FAB"),
                    spec("SLEEP", "19:30", "Ngủ đêm", "Một số bé 6 tháng có thể ngủ liền khoảng 8 tiếng/đêm.", "moon", "#5C7A8A")
            );
        }
        if (ageMonths < 13) {
            return List.of(
                    spec("WAKE", "07:00", "Bé thức dậy", "Giữ giờ dậy ổn định để tạo nhịp sinh học.", "sunny", "#FFB6C1"),
                    spec("MILK", "07:30", "Sữa/bữa sáng nhẹ", "Kết hợp sữa và đồ ăn phù hợp khả năng nhai nuốt.", "water", "#A7E4F5"),
                    spec("NAP", "09:30", "Giấc ngủ sáng", "Ban ngày thường còn khoảng 3-4 giờ ngủ.", "bed", "#74C0FC"),
                    spec("EAT", "11:30", "Bữa trưa ăn dặm", "Tăng độ thô dần, tránh thức ăn dễ hóc.", "restaurant", "#FF8FAB"),
                    spec("NAP", "13:30", "Giấc ngủ chiều", "Nhiều bé vẫn cần 2 giấc ngủ ngày.", "bed", "#74C0FC"),
                    spec("PLAY", "15:30", "Chơi vận động", "Bò, đứng bám, bập bẹ và trò chơi tương tác cùng người lớn.", "happy", "#FFD166"),
                    spec("EAT", "17:30", "Bữa tối nhẹ", "Ăn theo gia đình với kết cấu an toàn cho bé.", "restaurant", "#FF8FAB"),
                    spec("BATH", "18:30", "Tắm và thư giãn", "Rút ngắn kích thích 30-60 phút trước giờ ngủ.", "sparkles", "#FF8FAB"),
                    spec("SLEEP", "19:30", "Ngủ đêm", "Giấc đêm có thể kéo dài 9-12 tiếng, nhưng mọc răng/phát triển kỹ năng có thể làm bé khó ngủ.", "moon", "#5C7A8A")
            );
        }
        if (ageMonths < 19) {
            return List.of(
                    spec("WAKE", "07:00", "Bé thức dậy", "Giờ dậy đều giúp bữa ăn và giấc ngủ dễ đoán hơn.", "sunny", "#FFB6C1"),
                    spec("EAT", "07:30", "Bữa sáng", "Ăn đa dạng nhóm chất, để bé tập tự bốc/xúc.", "restaurant", "#FF8FAB"),
                    spec("PLAY", "09:00", "Chơi vận động", "Đi men, đi bộ, leo bậc thấp có giám sát.", "happy", "#FFD166"),
                    spec("NAP", "11:30", "Giấc ngủ ngày", "1-2 giấc tùy bé, tổng ngủ mục tiêu 11-14 giờ/ngày.", "bed", "#74C0FC"),
                    spec("EAT", "13:30", "Bữa trưa", "Bữa gia đình cắt nhỏ, mềm, ít muối.", "restaurant", "#FF8FAB"),
                    spec("PLAY", "15:30", "Chơi học ngôn ngữ", "Đọc sách, gọi tên đồ vật, hát bài ngắn.", "book", "#FFD166"),
                    spec("EAT", "17:30", "Bữa tối", "Tôn trọng tín hiệu no, tránh dùng màn hình để ép ăn.", "restaurant", "#FF8FAB"),
                    spec("SLEEP", "20:00", "Ngủ đêm", "Routine ngắn và lặp lại: tắm, sách, đèn dịu.", "moon", "#5C7A8A")
            );
        }
        return List.of(
                spec("WAKE", "07:00", "Bé thức dậy", "Nhịp ngày đêm ổn định hỗ trợ tâm trạng và ăn uống.", "sunny", "#FFB6C1"),
                spec("EAT", "07:30", "Bữa sáng", "Ăn cùng gia đình, tập dùng muỗng/cốc phù hợp.", "restaurant", "#FF8FAB"),
                spec("PLAY", "09:00", "Vận động ngoài trời", "Ưu tiên chơi chủ động, hạn chế ngồi lâu.", "happy", "#FFD166"),
                spec("EAT", "11:30", "Bữa trưa", "Đa dạng thực phẩm, canh chừng nguy cơ hóc.", "restaurant", "#FF8FAB"),
                spec("NAP", "12:30", "Giấc ngủ trưa", "Đa số bé 19-24 tháng cần 1 giấc ngủ ngày.", "bed", "#74C0FC"),
                spec("PLAY", "15:30", "Chơi sáng tạo", "Xếp hình, vẽ, đọc sách tranh, nói chuyện cùng bé.", "color-palette", "#FFD166"),
                spec("EAT", "17:30", "Bữa tối", "Giữ bữa tối không quá sát giờ ngủ.", "restaurant", "#FF8FAB"),
                spec("SLEEP", "20:00", "Ngủ đêm", "Tránh màn hình trước ngủ, giữ phòng ngủ yên tĩnh.", "moon", "#5C7A8A")
        );
    }

    private BabyRoutineDayResponse buildDayResponse(
            Profile profile,
            LocalDate routineDate,
            List<BabyRoutineEntry> entries,
            BabyRoutineAiAnalysisResponse aiInsight
    ) {
        int ageMonths = resolveAgeMonths(profile, routineDate);
        return BabyRoutineDayResponse.builder()
                .profileId(profile.getId())
                .profileName(profile.getName())
                .profileAgeMonths(ageMonths)
                .routineDate(routineDate.toString())
                .ageGroup(resolveAgeGroup(ageMonths))
                .sleepTargetMinHours(resolveSleepTargetMinHours(ageMonths))
                .sleepTargetMaxHours(resolveSleepTargetMaxHours(ageMonths))
                .feedingGuide(resolveFeedingGuide(ageMonths))
                .activityGuide(resolveActivityGuide(ageMonths))
                .sleepPrediction(buildSleepPrediction(profile, routineDate, entries))
                .entries(entries.stream().map(this::mapEntry).toList())
                .aiInsight(aiInsight)
                .build();
    }

    private BabyRoutineAiAnalysisResponse buildRuleBasedAnalysis(
            Profile profile,
            LocalDate routineDate,
            List<BabyRoutineEntry> entries,
            String summaryOverride
    ) {
        int ageMonths = resolveAgeMonths(profile, routineDate);
        long completedCount = entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getCompleted())).count();
        long napCount = entries.stream().filter(entry -> "NAP".equals(entry.getType())).count();
        long mealCount = entries.stream().filter(entry -> "EAT".equals(entry.getType())).count();
        String wakeTime = entries.stream()
                .filter(entry -> "WAKE".equals(entry.getType()))
                .findFirst()
                .map(entry -> formatTime(entry.getActualTime() != null ? entry.getActualTime() : entry.getPlannedTime()))
                .orElse("07:00");

        List<String> highlights = new ArrayList<>();
        highlights.add("Đã có " + completedCount + "/" + entries.size() + " mục được ghi nhận hôm nay.");
        highlights.add("Khung ngủ mục tiêu: "
                + resolveSleepTargetMinHours(ageMonths)
                + "-"
                + resolveSleepTargetMaxHours(ageMonths)
                + " giờ/ngày, tính cả ngủ ngày.");

        List<String> warnings = new ArrayList<>();
        if (ageMonths < 6 && mealCount > 0) {
            warnings.add("Bé dưới 6 tháng thường ưu tiên sữa mẹ/sữa công thức; ăn dặm nên theo chỉ dẫn chuyên môn.");
        }
        if (ageMonths >= 6 && mealCount == 0) {
            warnings.add("Từ khoảng 6 tháng, bé có thể bắt đầu ăn bổ sung bên cạnh sữa.");
        }
        if (napCount == 0) {
            warnings.add("Chưa có giấc ngủ ngày nào trong lịch, cần theo dõi dấu hiệu quá mệt.");
        }

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Ngày mai nên bắt đầu quanh " + wakeTime + " nếu đêm nay bé ngủ ổn.");
        BabyRoutineSleepPredictionResponse sleepPrediction = buildSleepPrediction(profile, routineDate, entries);
        if (sleepPrediction != null) {
            suggestions.add("Nếu bé ngủ lúc " + sleepPrediction.getSleepStartDateTime()
                    + ", dự kiến có thể dậy trong khoảng "
                    + sleepPrediction.getEarliestWakeDateTime()
                    + " đến "
                    + sleepPrediction.getLatestWakeDateTime()
                    + ".");
        }
        suggestions.add("Khi bé ngủ/ăn lệch giờ, hãy tick giờ thực tế để Đậu sức khỏe tự điều chỉnh lịch sau chính xác hơn.");
        suggestions.add("Nếu bé bỏ ăn, ngủ rất ít, sốt, lì bì hoặc có dấu hiệu bất thường, nên liên hệ bác sĩ.");

        String summary = summaryOverride != null
                ? summaryOverride
                : "Lịch hôm nay phù hợp nhóm " + resolveAgeGroup(ageMonths) + ". BabyOi sẽ ưu tiên quan sát giờ thực tế của bé thay vì ép một lịch quá cứng.";

        return BabyRoutineAiAnalysisResponse.builder()
                .title("Đậu sức khỏe nhận xét lịch sinh hoạt")
                .summary(summary)
                .recommendedTomorrowWakeTime(wakeTime)
                .highlights(highlights)
                .warnings(warnings)
                .suggestions(suggestions)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private String callGeminiForRoutine(Profile profile, LocalDate routineDate, List<BabyRoutineEntry> entries) {
        if (normalizedApiKey().isBlank()) {
            return null;
        }

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", normalizedApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Map<String, Object> requestBody = buildGeminiRequestBody(profile, routineDate, entries);

        for (String model : resolveTextModels()) {
            try {
                JsonNode response = webClient.post()
                        .uri("/{model}:generateContent", model)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                        .block();
                String text = extractOutputText(response);
                if (!text.isBlank()) {
                    return compactText(text, 700);
                }
            } catch (WebClientResponseException exception) {
                log.warn("Gemini routine analysis failed. status={}, model={}", exception.getStatusCode(), model);
            } catch (Exception exception) {
                log.warn("Gemini routine analysis fallback used. model={}", model, exception);
            }
        }
        return null;
    }

    private Map<String, Object> buildGeminiRequestBody(Profile profile, LocalDate routineDate, List<BabyRoutineEntry> entries) {
        StringBuilder routineText = new StringBuilder();
        entries.forEach(entry -> routineText
                .append(formatTime(entry.getActualTime() != null ? entry.getActualTime() : entry.getPlannedTime()))
                .append(" - ")
                .append(entry.getType())
                .append(" - ")
                .append(entry.getActivity())
                .append(Boolean.TRUE.equals(entry.getCompleted()) ? " (done)" : " (planned)")
                .append('\n'));

        String prompt = """
                You are BabyOi, a careful Vietnamese baby routine assistant.
                Analyze the routine for a baby from 0 to 24 months.
                Do not diagnose disease, prescribe medication, or claim certainty.
                Keep the answer in Vietnamese, warm, practical, and under 120 words.
                Mention that suggestions are reference only when needed.

                Baby name: %s
                Age months: %d
                Date: %s
                Routine:
                %s
                """.formatted(profile.getName(), resolveAgeMonths(profile, routineDate), routineDate, routineText);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(textPart));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.35);
        generationConfig.put("maxOutputTokens", 512);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private void shiftFutureEntries(BabyRoutineEntry completedEntry, long deltaMinutes, LocalDateTime now) {
        List<BabyRoutineEntry> entries = babyRoutineEntryRepository
                .findByProfile_IdAndRoutineDateAndStatusOrderByPlannedTimeAscIdAsc(
                        completedEntry.getProfile().getId(),
                        completedEntry.getRoutineDate(),
                        Constants.TABLE_STATUS.ACTIVE
                );
        boolean afterCompletedEntry = false;
        for (BabyRoutineEntry entry : entries) {
            if (entry.getId().equals(completedEntry.getId())) {
                afterCompletedEntry = true;
                continue;
            }
            if (afterCompletedEntry && !Boolean.TRUE.equals(entry.getCompleted())) {
                entry.setPlannedTime(entry.getPlannedTime().plusMinutes(deltaMinutes));
                entry.setNote("Đậu sức khỏe đã điều chỉnh theo giờ thực tế của bé");
                entry.setUpdatedAt(now);
            }
        }
        babyRoutineEntryRepository.saveAll(entries);
    }

    private List<BabyRoutineEntry> autoCompleteRoutineEntries(List<BabyRoutineEntry> entries, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDateTime overdueCutoff = now.minusHours(1);
        boolean changed = false;

        for (BabyRoutineEntry entry : entries) {
            if (Boolean.TRUE.equals(entry.getCompleted())) {
                continue;
            }
            if ("CUSTOM".equals(entry.getSource())) {
                continue;
            }

            boolean pastDay = entry.getRoutineDate().isBefore(today);
            boolean overdueToday = entry.getRoutineDate().isEqual(today)
                    && overdueCutoff.toLocalDate().isEqual(today)
                    && !entry.getPlannedTime().isAfter(overdueCutoff.toLocalTime());
            if (!pastDay && !overdueToday) {
                continue;
            }

            entry.setCompleted(true);
            entry.setActualTime(entry.getPlannedTime());
            entry.setNote(pastDay
                    ? "Đậu sức khỏe tự hoàn thành cuối ngày theo giờ dự kiến"
                    : "Đậu sức khỏe tự hoàn thành do quá hẹn 1 giờ");
            entry.setSource(pastDay ? "AUTO_END_OF_DAY" : "AUTO_OVERDUE");
            entry.setUpdatedAt(now);
            changed = true;
        }

        return changed ? babyRoutineEntryRepository.saveAll(entries) : entries;
    }

    private void validateRoutineDayCompleted(List<BabyRoutineEntry> entries) {
        boolean completed = !entries.isEmpty() && entries.stream()
                .allMatch(entry -> Boolean.TRUE.equals(entry.getCompleted()) && entry.getActualTime() != null);
        if (!completed) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần hoàn thành toàn bộ lịch hôm nay trước khi Đậu sức khỏe cập nhật lịch ngày mai"
            );
        }
    }

    private void validateCanCompleteRoutineEntry(BabyRoutineEntry entry) {
        LocalDate today = LocalDate.now(APP_ZONE);
        if (!entry.getRoutineDate().isAfter(today)) {
            return;
        }

        List<BabyRoutineEntry> todayEntries = getOrCreateRoutineEntries(entry.getProfile(), today);
        boolean todayCompleted = !todayEntries.isEmpty() && todayEntries.stream()
                .allMatch(todayEntry -> Boolean.TRUE.equals(todayEntry.getCompleted()));
        if (!todayCompleted) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần hoàn thành toàn bộ lịch hôm nay trước khi tích lịch ngày tiếp theo"
            );
        }
    }

    private int calculateRoutineShiftMinutes(List<BabyRoutineEntry> entries) {
        double averageShift = entries.stream()
                .filter(entry -> entry.getActualTime() != null)
                .mapToLong(entry -> Duration.between(entry.getPlannedTime(), entry.getActualTime()).toMinutes())
                .average()
                .orElse(0);
        int roundedToFiveMinutes = (int) Math.round(averageShift / 5.0) * 5;
        return Math.max(-120, Math.min(120, roundedToFiveMinutes));
    }

    private String formatSignedMinutes(int minutes) {
        if (minutes == 0) {
            return "giữ nguyên giờ";
        }
        int absMinutes = Math.abs(minutes);
        int hours = absMinutes / 60;
        int mins = absMinutes % 60;
        String value = (hours > 0 ? hours + " giờ" : "")
                + (hours > 0 && mins > 0 ? " " : "")
                + (mins > 0 ? mins + " phút" : "");
        return minutes > 0 ? "muộn hơn " + value : "sớm hơn " + value;
    }

    private Profile resolveChildProfile(Long profileId, LocalDate routineDate) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!Constants.TABLE_STATUS.ACTIVE.equals(profile.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile is not active");
        }
        validateCurrentUser(profile.getUser().getId());
        if (!"CHILD".equalsIgnoreCase(profile.getProfileType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Routine chỉ hỗ trợ hồ sơ bé");
        }
        if (profile.getDateOfBirth() == null || profile.getDateOfBirth().isAfter(routineDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày sinh của bé không hợp lệ");
        }
        if (routineDate.isBefore(resolveRoutineStartDate(profile))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lịch sinh hoạt chỉ được tính từ ngày tạo hồ sơ"
            );
        }
        int ageMonths = resolveAgeMonths(profile, routineDate);
        if (ageMonths > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Routine hiện chỉ hỗ trợ bé từ 0 đến 24 tháng");
        }
        return profile;
    }

    private Profile resolveChildProfileForRoutine(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!Constants.TABLE_STATUS.ACTIVE.equals(profile.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile is not active");
        }
        validateCurrentUser(profile.getUser().getId());
        if (!"CHILD".equalsIgnoreCase(profile.getProfileType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Routine chỉ hỗ trợ hồ sơ bé");
        }
        if (profile.getDateOfBirth() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày sinh của bé không hợp lệ");
        }
        return profile;
    }

    private LocalDate resolveRoutineStartDate(Profile profile) {
        LocalDate profileCreatedDate = profile.getCreatedAt() != null
                ? profile.getCreatedAt().toLocalDate()
                : profile.getDateOfBirth();
        return profileCreatedDate.isAfter(profile.getDateOfBirth())
                ? profileCreatedDate
                : profile.getDateOfBirth();
    }

    private int resolveAgeMonths(Profile profile, LocalDate date) {
        return Math.max(0, (int) ChronoUnit.MONTHS.between(profile.getDateOfBirth(), date));
    }

    private String resolveAgeGroup(int ageMonths) {
        if (ageMonths < 3) {
            return "0-2 tháng";
        }
        if (ageMonths < 6) {
            return "3-5 tháng";
        }
        if (ageMonths < 9) {
            return "6-8 tháng";
        }
        if (ageMonths < 13) {
            return "9-12 tháng";
        }
        if (ageMonths < 19) {
            return "13-18 tháng";
        }
        return "19-24 tháng";
    }

    private int resolveSleepTargetMinHours(int ageMonths) {
        if (ageMonths < 3) {
            return 17;
        }
        if (ageMonths < 6) {
            return 14;
        }
        if (ageMonths < 9) {
            return 13;
        }
        if (ageMonths < 13) {
            return 12;
        }
        return 11;
    }

    private int resolveSleepTargetMaxHours(int ageMonths) {
        if (ageMonths < 3) {
            return 18;
        }
        if (ageMonths < 6) {
            return 16;
        }
        if (ageMonths < 9) {
            return 15;
        }
        if (ageMonths < 13) {
            return 16;
        }
        return 14;
    }

    private String resolveFeedingGuide(int ageMonths) {
        if (ageMonths < 6) {
            return "Sữa mẹ hoặc sữa công thức là nguồn dinh dưỡng chính; chưa nên ép ăn dặm.";
        }
        if (ageMonths < 9) {
            return "Bắt đầu ăn bổ sung mềm, ít một, ưu tiên thực phẩm giàu sắt và vẫn duy trì sữa.";
        }
        if (ageMonths < 13) {
            return "Tăng dần kết cấu và số bữa ăn dặm, tiếp tục sữa theo nhu cầu.";
        }
        return "Ăn đa dạng cùng gia đình với kết cấu an toàn, ít muối/đường, tôn trọng tín hiệu no.";
    }

    private String resolveActivityGuide(int ageMonths) {
        if (ageMonths < 6) {
            return "Vận động nhẹ, nằm sấp có giám sát và tương tác mặt-đối-mặt.";
        }
        if (ageMonths < 13) {
            return "Cho bé bò, ngồi, đứng bám theo khả năng; ưu tiên chơi chủ động và an toàn.";
        }
        return "Khuyến khích đi bộ, leo bậc thấp có giám sát, đọc sách và chơi tương tác.";
    }

    private BabyRoutineSleepPredictionResponse buildSleepPrediction(Profile profile, LocalDate routineDate, List<BabyRoutineEntry> entries) {
        BabyRoutineEntry sleepEntry = entries.stream()
                .filter(entry -> "SLEEP".equals(entry.getType()))
                .findFirst()
                .orElse(null);
        if (sleepEntry == null) {
            return null;
        }

        int ageMonths = resolveAgeMonths(profile, routineDate);
        SleepWindow sleepWindow = resolveNightSleepWindow(ageMonths);
        LocalTime sleepStartTime = sleepEntry.getActualTime() != null ? sleepEntry.getActualTime() : sleepEntry.getPlannedTime();
        LocalDateTime sleepStartDateTime = LocalDateTime.of(routineDate, sleepStartTime);
        LocalDateTime earliestWakeDateTime = sleepStartDateTime.plusMinutes(sleepWindow.minMinutes());
        LocalDateTime likelyWakeDateTime = sleepStartDateTime.plusMinutes(sleepWindow.likelyMinutes());
        LocalDateTime latestWakeDateTime = sleepStartDateTime.plusMinutes(sleepWindow.maxMinutes());
        long minutesUntilLikelyWake = Duration.between(LocalDateTime.now(APP_ZONE), likelyWakeDateTime).toMinutes();

        return BabyRoutineSleepPredictionResponse.builder()
                .sleepType("NIGHT_SLEEP")
                .sleepStartDateTime(formatDateTime(sleepStartDateTime))
                .earliestWakeDateTime(formatDateTime(earliestWakeDateTime))
                .likelyWakeDateTime(formatDateTime(likelyWakeDateTime))
                .latestWakeDateTime(formatDateTime(latestWakeDateTime))
                .expectedSleepMinutesMin(sleepWindow.minMinutes())
                .expectedSleepMinutesLikely(sleepWindow.likelyMinutes())
                .expectedSleepMinutesMax(sleepWindow.maxMinutes())
                .minutesUntilLikelyWake(minutesUntilLikelyWake)
                .nightEvents(buildNightEventPredictions(ageMonths, sleepStartDateTime))
                .explanation(buildSleepPredictionExplanation(ageMonths, sleepWindow, sleepEntry.getActualTime() != null))
                .build();
    }

    private List<BabyRoutineNightEventPredictionResponse> buildNightEventPredictions(int ageMonths, LocalDateTime sleepStartDateTime) {
        if (ageMonths < 3) {
            return List.of(
                    nightEvent("NIGHT_FEED", "Cữ đêm 1", sleepStartDateTime, 120, 180, 240,
                            "Bé 0-2 tháng thường thức sau 2-4 giờ để bú hoặc được vỗ về."),
                    nightEvent("NIGHT_FEED", "Cữ đêm 2", sleepStartDateTime, 300, 390, 480,
                            "Cữ đêm tiếp theo có thể xuất hiện nếu bé chưa ngủ đủ hoặc đói."),
                    nightEvent("EARLY_MORNING_FEED", "Cữ gần sáng", sleepStartDateTime, 480, 540, 600,
                            "Gần sáng bé có thể bú thêm rồi ngủ tiếp hoặc bắt đầu ngày mới.")
            );
        }
        if (ageMonths < 6) {
            return List.of(
                    nightEvent("NIGHT_FEED", "Cữ đêm có thể xảy ra", sleepStartDateTime, 300, 360, 480,
                            "Bé 3-5 tháng có thể ngủ một mạch khoảng 6 tiếng, nhưng thức 1-2 lần ban đêm vẫn bình thường."),
                    nightEvent("EARLY_MORNING_WAKE", "Dậy sáng sớm", sleepStartDateTime, 540, 600, 720,
                            "Nếu đêm ổn, bé có thể dậy vào khoảng sáng sớm sau giấc ngủ dài.")
            );
        }
        if (ageMonths < 9) {
            return List.of(
                    nightEvent("POSSIBLE_WAKE", "Có thể dậy giữa đêm", sleepStartDateTime, 360, 480, 540,
                            "Một số bé 6-8 tháng có thể ngủ liền khoảng 8 tiếng, nhưng vẫn có thể thức ngắn khi đói, mọc răng hoặc cần trấn an."),
                    nightEvent("EARLY_MORNING_WAKE", "Dậy buổi sáng", sleepStartDateTime, 540, 600, 660,
                            "Sau giấc đêm dài, bé thường dậy vào sáng sớm; nếu còn buồn ngủ có thể ngủ lại ngắn.")
            );
        }
        if (ageMonths < 13) {
            return List.of(
                    nightEvent("SLEEP_REGRESSION_WAKE", "Có thể thức ngắn", sleepStartDateTime, 540, 600, 720,
                            "Bé 9-12 tháng thường ngủ đêm 9-12 tiếng, nhưng phát triển kỹ năng, mọc răng hoặc bám mẹ có thể làm bé thức ngắn."),
                    nightEvent("MORNING_WAKE", "Dậy buổi sáng", sleepStartDateTime, 600, 660, 720,
                            "Nếu giấc đêm ổn, đây là khoảng bé có thể dậy để bắt đầu ngày mới.")
            );
        }
        return List.of(
                nightEvent("MORNING_WAKE", "Dậy buổi sáng", sleepStartDateTime, 600, 660, 720,
                        "Bé lớn hơn thường có giấc đêm ổn hơn; mốc này giúp mẹ dự đoán giờ dậy sáng.")
        );
    }

    private BabyRoutineNightEventPredictionResponse nightEvent(
            String type,
            String label,
            LocalDateTime sleepStartDateTime,
            int earliestOffsetMinutes,
            int likelyOffsetMinutes,
            int latestOffsetMinutes,
            String note
    ) {
        return BabyRoutineNightEventPredictionResponse.builder()
                .type(type)
                .label(label)
                .earliestDateTime(formatDateTime(sleepStartDateTime.plusMinutes(earliestOffsetMinutes)))
                .likelyDateTime(formatDateTime(sleepStartDateTime.plusMinutes(likelyOffsetMinutes)))
                .latestDateTime(formatDateTime(sleepStartDateTime.plusMinutes(latestOffsetMinutes)))
                .note(note)
                .build();
    }

    private SleepWindow resolveNightSleepWindow(int ageMonths) {
        if (ageMonths < 3) {
            return new SleepWindow(120, 180, 240);
        }
        if (ageMonths < 6) {
            return new SleepWindow(300, 360, 480);
        }
        if (ageMonths < 9) {
            return new SleepWindow(360, 480, 540);
        }
        if (ageMonths < 13) {
            return new SleepWindow(540, 600, 720);
        }
        return new SleepWindow(600, 660, 720);
    }

    private String buildSleepPredictionExplanation(int ageMonths, SleepWindow sleepWindow, boolean basedOnActualTime) {
        String source = basedOnActualTime ? "giờ ngủ thực tế vừa ghi nhận" : "giờ ngủ dự kiến trong lịch";
        if (ageMonths < 4) {
            return "Dự đoán dựa trên " + source + ". Bé 0-3 tháng thường ngủ theo cụm ngắn và có thể dậy sau "
                    + formatDurationRange(sleepWindow) + " để bú hoặc cần vỗ về; BabyOi cũng gợi ý các mốc cữ đêm dự kiến bên dưới.";
        }
        if (ageMonths < 6) {
            return "Dự đoán dựa trên " + source + ". Bé 4-5 tháng có thể có giấc đêm dài hơn, nhưng vẫn có thể thức ngắn giữa đêm; BabyOi sẽ hiện cữ đêm dự kiến nếu có.";
        }
        return "Dự đoán dựa trên " + source + ". Ở độ tuổi này, nếu ngày không quá mệt và routine ổn, bé thường có giấc đêm dài khoảng "
                + formatDurationRange(sleepWindow) + ".";
    }

    private String formatDurationRange(SleepWindow sleepWindow) {
        return formatMinutesAsHours(sleepWindow.minMinutes()) + "-" + formatMinutesAsHours(sleepWindow.maxMinutes());
    }

    private String formatMinutesAsHours(int minutes) {
        if (minutes % 60 == 0) {
            return (minutes / 60) + " giờ";
        }
        return (minutes / 60) + " giờ " + (minutes % 60) + " phút";
    }

    private BabyRoutineEntryResponse mapEntry(BabyRoutineEntry entry) {
        return BabyRoutineEntryResponse.builder()
                .id(entry.getId())
                .profileId(entry.getProfile().getId())
                .routineDate(entry.getRoutineDate().toString())
                .type(entry.getType())
                .time(formatTime(entry.getPlannedTime()))
                .actualTime(entry.getActualTime() != null ? formatTime(entry.getActualTime()) : null)
                .activity(entry.getActivity())
                .note(entry.getNote())
                .icon(entry.getIcon())
                .color(entry.getColor())
                .completed(entry.getCompleted())
                .source(entry.getSource())
                .build();
    }

    private int findEntryIndex(List<BabyRoutineEntry> entries, Long entryId) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).getId().equals(entryId)) {
                return index;
            }
        }
        return -1;
    }

    private RoutineSpec spec(String type, String time, String activity, String note, String icon, String color) {
        return new RoutineSpec(type, time, activity, note, icon, color);
    }

    private LocalDate parseRoutineDate(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routineDate must use yyyy-MM-dd");
        }
    }

    private LocalTime parseRequiredTime(String time, String fieldName) {
        try {
            return LocalTime.parse(normalizeTimeInput(time));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must use HH:mm");
        }
    }

    private LocalTime parseOptionalTime(String time, LocalTime fallback) {
        if (time == null || time.isBlank()) {
            return fallback.withSecond(0).withNano(0);
        }
        return parseRequiredTime(time, "actualTime").withSecond(0).withNano(0);
    }

    private String normalizeTimeInput(String time) {
        String value = time == null ? "" : time.trim();
        if (value.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = value.split(":");
            return "%02d:%02d".formatted(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 3) {
            digits = "0" + digits;
        }
        if (digits.length() == 4) {
            return digits.substring(0, 2) + ":" + digits.substring(2);
        }
        throw new IllegalArgumentException("Invalid time");
    }

    private void validateNewRoutineTime(List<BabyRoutineEntry> entries, LocalTime plannedTime) {
        List<BabyRoutineEntry> sortedEntries = entries.stream()
                .sorted(Comparator.comparing(BabyRoutineEntry::getPlannedTime).thenComparing(BabyRoutineEntry::getId))
                .toList();
        BabyRoutineEntry previous = null;
        BabyRoutineEntry next = null;
        for (BabyRoutineEntry entry : sortedEntries) {
            if (entry.getPlannedTime().equals(plannedTime)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung gio nay da co trong lich cua be");
            }
            if (entry.getPlannedTime().isBefore(plannedTime)) {
                previous = entry;
                continue;
            }
            next = entry;
            break;
        }
        if (previous != null && !plannedTime.isAfter(previous.getPlannedTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng cập nhật lịch trình theo đúng lịch");
        }
        if (next != null && !plannedTime.isBefore(next.getPlannedTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng cập nhật lịch trình theo đúng lịch");
        }
    }

    private void validateActualTimeAfterPrevious(BabyRoutineEntry entry, LocalTime actualTime) {
        List<BabyRoutineEntry> entries = babyRoutineEntryRepository
                .findByProfile_IdAndRoutineDateAndStatusOrderByPlannedTimeAscIdAsc(
                        entry.getProfile().getId(),
                        entry.getRoutineDate(),
                        Constants.TABLE_STATUS.ACTIVE
                );
        int index = findEntryIndex(entries, entry.getId());
        if (index <= 0) {
            return;
        }
        BabyRoutineEntry previous = entries.get(index - 1);
        LocalTime previousTime = previous.getActualTime() != null ? previous.getActualTime() : previous.getPlannedTime();
        if (!actualTime.isAfter(previousTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng cập nhật lịch trình theo đúng lịch"
            );
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String formatTime(LocalTime time) {
        return time == null ? null : "%02d:%02d".formatted(time.getHour(), time.getMinute());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    private void validateEntryActive(BabyRoutineEntry entry) {
        if (!Constants.TABLE_STATUS.ACTIVE.equals(entry.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine entry not found");
        }
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

    private String normalizedApiKey() {
        return apiKey != null ? apiKey.replaceAll("\\s+", "") : "";
    }

    private List<String> resolveTextModels() {
        if (textModels == null || textModels.isBlank()) {
            return List.of("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite");
        }
        return List.of(textModels.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private long resolveTimeoutSeconds() {
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 20L;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        JsonNode candidates = response.get("candidates");
        if (candidates != null && candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (parts != null && parts.isArray()) {
                    for (JsonNode part : parts) {
                        JsonNode text = part.get("text");
                        if (text != null && text.isTextual()) {
                            builder.append(text.asText());
                        }
                    }
                }
            }
        }
        return builder.toString().trim();
    }

    private String compactText(String text, int maxLength) {
        String compact = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength);
    }

    private record RoutineSpec(String type, String time, String activity, String note, String icon, String color) {
    }

    private record SleepWindow(int minMinutes, int likelyMinutes, int maxMinutes) {
    }

    private record PredictedNightEvent(
            String type,
            String label,
            LocalDateTime likelyDateTime,
            String note,
            boolean addSleepBack
    ) {
    }

    private record PredictedRoutineSpec(
            String type,
            LocalDateTime dateTime,
            String activity,
            String note,
            String icon,
            String color,
            String sourceDate
    ) {
    }
}
