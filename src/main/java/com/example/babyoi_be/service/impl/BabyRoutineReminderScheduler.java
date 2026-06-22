package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.entity.BabyRoutineEntry;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.repository.BabyRoutineEntryRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.service.BabyRoutineService;
import com.example.babyoi_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BabyRoutineReminderScheduler {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int REMINDER_WINDOW_START_MINUTES = 20;
    private static final int REMINDER_WINDOW_END_MINUTES = 25;

    private final BabyRoutineEntryRepository babyRoutineEntryRepository;
    private final ProfileRepository profileRepository;
    private final BabyRoutineService babyRoutineService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void ensureTodayRoutineEntries() {
        LocalDate today = LocalDate.now(APP_ZONE);
        int createdOrConfirmed = 0;
        for (Profile profile : profileRepository.findByProfileTypeAndStatus("CHILD", Constants.TABLE_STATUS.ACTIVE)) {
            if (!supportsRoutine(profile, today)) {
                continue;
            }
            try {
                babyRoutineService.getDailyRoutine(profile.getId(), today);
                createdOrConfirmed++;
            } catch (Exception exception) {
                log.warn("Could not prepare baby routine for profileId={}", profile.getId(), exception);
            }
        }
        if (createdOrConfirmed > 0) {
            log.info("Prepared baby routine entries for {} profiles on {}", createdOrConfirmed, today);
        }
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendUpcomingRoutineReminders() {
        LocalDateTime now = LocalDateTime.now(APP_ZONE).withSecond(0).withNano(0);
        LocalDateTime windowStart = now.plusMinutes(REMINDER_WINDOW_START_MINUTES);
        LocalDateTime windowEnd = now.plusMinutes(REMINDER_WINDOW_END_MINUTES);

        int created = 0;
        for (BabyRoutineEntry entry : findUpcomingEntries(windowStart, windowEnd)) {
            if (createReminder(entry)) {
                created++;
            }
        }
        if (created > 0) {
            log.info("Created {} baby routine reminders for window {} - {}", created, windowStart, windowEnd);
        }
    }

    private List<BabyRoutineEntry> findUpcomingEntries(LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<BabyRoutineEntry> entries = new ArrayList<>();
        LocalDate startDate = windowStart.toLocalDate();
        LocalDate endDate = windowEnd.toLocalDate();

        if (startDate.equals(endDate)) {
            entries.addAll(findEntriesForDate(startDate, windowStart.toLocalTime(), windowEnd.toLocalTime()));
            return entries;
        }

        entries.addAll(findEntriesForDate(startDate, windowStart.toLocalTime(), LocalTime.MAX));
        entries.addAll(findEntriesForDate(endDate, LocalTime.MIN, windowEnd.toLocalTime()));
        return entries;
    }

    private List<BabyRoutineEntry> findEntriesForDate(LocalDate date, LocalTime fromTime, LocalTime toTime) {
        return babyRoutineEntryRepository
                .findByRoutineDateAndStatusAndPlannedTimeGreaterThanEqualAndPlannedTimeBeforeOrderByPlannedTimeAscIdAsc(
                        date,
                        Constants.TABLE_STATUS.ACTIVE,
                        fromTime,
                        toTime
                );
    }

    private boolean createReminder(BabyRoutineEntry entry) {
        Profile profile = entry.getProfile();
        if (profile == null || profile.getUser() == null) {
            return false;
        }
        if (!isChildProfile(profile) || !supportsRoutine(profile, entry.getRoutineDate())) {
            return false;
        }

        String childName = normalize(profile.getName(), null);
        String childLabel = childName == null ? "bé" : "bé " + childName;
        String sentenceChildLabel = childName == null ? "Bé" : "Bé " + childName;
        String activity = normalize(entry.getActivity(), "hoạt động theo lịch");
        String plannedTime = formatTime(entry.getPlannedTime());
        String title = "Sắp đến lịch sinh hoạt của " + childLabel;
        String body = sentenceChildLabel + " có lịch: " + activity + " lúc " + plannedTime
                + ". Mẹ chuẩn bị trước nhé.";
        String dataJson = String.format(
                "{\"route\":\"/phattrien/daily-schedule\",\"screen\":\"BabyRoutine\",\"entryId\":%d,\"profileId\":%d,\"routineDate\":\"%s\",\"plannedTime\":\"%s\",\"type\":\"%s\"}",
                entry.getId(),
                profile.getId(),
                entry.getRoutineDate(),
                plannedTime,
                escapeJson(entry.getType())
        );
        String reminderKey = "ROUTINE:" + entry.getId() + ":" + entry.getRoutineDate() + ":" + plannedTime;

        return notificationService.createReminderNotification(
                profile.getUser().getId(),
                Constants.NOTIFICATION_TYPE.ROUTINE_REMINDER,
                title,
                body,
                dataJson,
                Constants.NOTIFICATION_PRIORITY.NORMAL,
                "BABY_ROUTINE_ENTRY",
                entry.getId(),
                reminderKey
        );
    }

    private boolean supportsRoutine(Profile profile, LocalDate date) {
        return profile.getDateOfBirth() != null
                && !profile.getDateOfBirth().isAfter(date)
                && profile.getDateOfBirth().plusMonths(25).isAfter(date);
    }

    private boolean isChildProfile(Profile profile) {
        return "CHILD".equalsIgnoreCase(profile.getProfileType());
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String formatTime(LocalTime time) {
        return time == null ? "" : "%02d:%02d".formatted(time.getHour(), time.getMinute());
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
