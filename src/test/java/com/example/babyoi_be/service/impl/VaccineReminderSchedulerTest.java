package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.VaccineRuleConstants;
import com.example.babyoi_be.domain.entity.ChildVaccineDisease;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.ProfileVaccineDiseaseStatus;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.repository.NotificationRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.ProfileVaccineDiseaseStatusRepository;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineReminderSchedulerTest {

    @Mock
    private VaccineRecordRepository vaccineRecordRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileVaccineDiseaseStatusRepository profileVaccineDiseaseStatusRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VaccineReminderScheduler scheduler;

    @Test
    void sendOverdueRemindersCreatesReminderAtConfiguredMilestone() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 22, 11, 50);
        VaccineRecord record = overdueRecord(now.toLocalDate().minusDays(1));
        Profile profile = childProfile();

        when(vaccineRecordRepository.findByInjectionDateAndStatus(any(LocalDate.class), eq(Constants.TABLE_STATUS.PENDING)))
                .thenReturn(List.of());
        when(vaccineRecordRepository.findByInjectionDateAndStatus(now.toLocalDate().minusDays(1), Constants.TABLE_STATUS.PENDING))
                .thenReturn(List.of(record));
        when(vaccineRecordRepository.findByInjectionDateBeforeAndStatus(now.toLocalDate().minusDays(30), Constants.TABLE_STATUS.PENDING))
                .thenReturn(List.of());
        when(profileRepository.findById(record.getProfileId())).thenReturn(Optional.of(profile));
        when(notificationService.createReminderNotification(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(true);

        scheduler.sendOverdueReminders(now);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> reminderKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createReminderNotification(
                eq(profile.getUser().getId()),
                eq(Constants.NOTIFICATION_TYPE.VACCINE_REMINDER),
                titleCaptor.capture(),
                any(),
                any(),
                any(),
                eq("VACCINE_RECORD"),
                eq(record.getId()),
                reminderKeyCaptor.capture()
        );
        assertThat(titleCaptor.getValue()).contains("quá hạn 1 ngày");
        assertThat(reminderKeyCaptor.getValue()).endsWith(":OD1");
    }

    @Test
    void sendOverdueRemindersStopsDiseaseScheduleAfterThirtyDays() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 22, 11, 50);
        VaccineRecord record = overdueRecord(now.toLocalDate().minusDays(31));
        Profile profile = childProfile();

        when(vaccineRecordRepository.findByInjectionDateAndStatus(any(LocalDate.class), eq(Constants.TABLE_STATUS.PENDING)))
                .thenReturn(List.of());
        when(vaccineRecordRepository.findByInjectionDateBeforeAndStatus(now.toLocalDate().minusDays(30), Constants.TABLE_STATUS.PENDING))
                .thenReturn(List.of(record));
        when(profileRepository.findById(record.getProfileId())).thenReturn(Optional.of(profile));
        when(profileVaccineDiseaseStatusRepository.findByProfileIdAndDiseaseId(record.getProfileId(), record.getDisease().getId()))
                .thenReturn(Optional.empty());
        when(profileVaccineDiseaseStatusRepository.save(any(ProfileVaccineDiseaseStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.sendOverdueReminders(now);

        ArgumentCaptor<ProfileVaccineDiseaseStatus> statusCaptor = ArgumentCaptor.forClass(ProfileVaccineDiseaseStatus.class);
        verify(profileVaccineDiseaseStatusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getStatus()).isEqualTo(VaccineRuleConstants.DISEASE_SCHEDULE_STATUS.STOPPED);
        assertThat(statusCaptor.getValue().getStoppedDoseOrder()).isEqualTo(record.getDoseOrder());
    }

    private VaccineRecord overdueRecord(LocalDate injectionDate) {
        return VaccineRecord.builder()
                .id(100L)
                .profileId(20L)
                .disease(ChildVaccineDisease.builder()
                        .id(30L)
                        .name("Cúm mùa")
                        .build())
                .doseOrder(2)
                .injectionDate(injectionDate)
                .status(Constants.TABLE_STATUS.PENDING)
                .build();
    }

    private Profile childProfile() {
        return Profile.builder()
                .id(20L)
                .user(Users.builder()
                        .id(10L)
                        .userName("parent")
                        .email("parent@example.com")
                        .passwordHash("password")
                        .status(Constants.TABLE_STATUS.ACTIVE)
                        .emailVerified(true)
                        .build())
                .name("Bin")
                .profileType("CHILD")
                .status(Constants.TABLE_STATUS.ACTIVE)
                .build();
    }
}
