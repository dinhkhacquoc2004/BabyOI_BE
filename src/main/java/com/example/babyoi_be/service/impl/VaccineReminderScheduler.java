package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.repository.NotificationRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaccineReminderScheduler {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int[] EARLY_REMINDER_DAYS = {7, 3};

    private final VaccineRecordRepository vaccineRecordRepository;
    private final ProfileRepository profileRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */10 6 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendDueTodayReminders() {
        sendRemindersForDate(LocalDate.now(VIETNAM_ZONE), 0, null);
    }

    @Scheduled(cron = "0 */10 8-9 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendEarlyReminders() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        int currentSlot = (now.getHour() - 8) * 6 + now.getMinute() / 10;
        LocalDate today = now.toLocalDate();
        for (int daysBefore : EARLY_REMINDER_DAYS) {
            sendRemindersForDate(today.plusDays(daysBefore), daysBefore, currentSlot);
        }
    }

    @Scheduled(cron = "0 15 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void maintainNotificationHistory() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        int archived = notificationRepository.archiveReadBefore(now.minusDays(30), now);
        int deleted = notificationRepository.deleteArchivedBefore(now.minusDays(180));
        if (archived > 0 || deleted > 0) {
            log.info("Notification maintenance archived={} deleted={}", archived, deleted);
        }
    }

    private void sendRemindersForDate(LocalDate injectionDate, int daysBefore, Integer currentSlot) {
        List<VaccineRecord> records = vaccineRecordRepository.findByInjectionDateAndStatus(
                injectionDate,
                Constants.TABLE_STATUS.PENDING
        );
        for (VaccineRecord record : records) {
            if (currentSlot != null && reminderSlot(record) > currentSlot) {
                continue;
            }
            createReminder(record, daysBefore);
        }
    }

    private int reminderSlot(VaccineRecord record) {
        long userSeed = profileRepository.findById(record.getProfileId())
                .map(profile -> profile.getUser().getId())
                .orElse(0L);
        return Math.floorMod(Long.hashCode(userSeed * 31 + record.getId()), 12);
    }

    private void createReminder(VaccineRecord record, int daysBefore) {
        Profile profile = profileRepository.findById(record.getProfileId()).orElse(null);
        if (profile == null || profile.getUser() == null) {
            return;
        }

        String profileLabel = resolveProfileLabel(profile, false);
        String sentenceProfileLabel = resolveProfileLabel(profile, true);
        String immunizationName = resolveImmunizationName(record);
        String doseLabel = record.getDoseOrder() == null ? "mũi theo lịch" : "mũi " + record.getDoseOrder();
        String title = daysBefore == 0
                ? "Lịch tiêm hôm nay của " + profileLabel
                : "Còn " + daysBefore + " ngày đến lịch tiêm của " + profileLabel;
        String body = daysBefore == 0
                ? "Hôm nay " + profileLabel + " tiêm " + immunizationName + ", " + doseLabel
                + ". Mẹ nhớ kiểm tra giấy tờ và giờ hẹn nhé."
                : sentenceProfileLabel + " sẽ tiêm " + immunizationName + ", " + doseLabel + " vào "
                + record.getInjectionDate().format(DISPLAY_DATE) + ". Mẹ có thể sắp xếp thời gian từ bây giờ.";
        String dataJson = record.getDisease() != null
                ? String.format(
                        "{\"route\":\"/lichtiem/chitietbenh\",\"screen\":\"VaccineRecordDetail\",\"recordId\":%d,\"profileId\":%d,\"diseaseId\":%d,\"daysBefore\":%d}",
                        record.getId(), record.getProfileId(), record.getDisease().getId(), daysBefore
                )
                : String.format(
                        "{\"route\":\"/lichtiem/themmuitiem\",\"screen\":\"VaccineRecordDetail\",\"mode\":\"edit\",\"recordId\":%d,\"profileId\":%d,\"daysBefore\":%d}",
                        record.getId(), record.getProfileId(), daysBefore
                );
        String reminderKey = "VACCINE:" + record.getId() + ":" + record.getInjectionDate() + ":D" + daysBefore;

        notificationService.createReminderNotification(
                profile.getUser().getId(),
                Constants.NOTIFICATION_TYPE.VACCINE_REMINDER,
                title,
                body,
                dataJson,
                daysBefore == 0 ? Constants.NOTIFICATION_PRIORITY.HIGH : Constants.NOTIFICATION_PRIORITY.NORMAL,
                "VACCINE_RECORD",
                record.getId(),
                reminderKey
        );
    }

    private String resolveProfileLabel(Profile profile, boolean sentenceStart) {
        String subject = isMotherProfile(profile)
                ? (sentenceStart ? "Mẹ" : "mẹ")
                : (sentenceStart ? "Bé" : "bé");
        String name = normalize(profile.getName());
        return name == null ? subject : subject + " " + name;
    }

    private boolean isMotherProfile(Profile profile) {
        return "MOTHER".equalsIgnoreCase(profile.getProfileType());
    }

    private String resolveImmunizationName(VaccineRecord record) {
        String diseaseName = record.getDisease() != null ? normalize(record.getDisease().getName()) : null;
        String vaccineName = record.getVaccine() != null ? normalize(record.getVaccine().getName()) : null;
        if (diseaseName != null && vaccineName != null && !diseaseName.equalsIgnoreCase(vaccineName)) {
            return diseaseName + " (" + vaccineName + ")";
        }
        if (diseaseName != null) {
            return diseaseName;
        }
        if (vaccineName != null) {
            return vaccineName;
        }
        return "vaccine theo lịch";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
