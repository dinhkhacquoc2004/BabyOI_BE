package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.NotificationSettingRequest;
import com.example.babyoi_be.domain.dto.respone.NotificationSettingResponse;
import com.example.babyoi_be.domain.entity.Notification;
import com.example.babyoi_be.domain.entity.NotificationSetting;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.DeviceTokenRepository;
import com.example.babyoi_be.repository.NotificationRepository;
import com.example.babyoi_be.repository.NotificationSettingRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long USER_ID = 10L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private Executor applicationTaskExecutor;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Users user;

    @BeforeEach
    void setUpSecurityContext() {
        user = Users.builder()
                .id(USER_ID)
                .email("parent@example.com")
                .userName("parent")
                .passwordHash("password")
                .status(Constants.TABLE_STATUS.ACTIVE)
                .emailVerified(true)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateCategorySettingTogglesInAppAndPushTogether() {
        NotificationSetting setting = defaultSetting();
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));
        when(notificationSettingRepository.save(any(NotificationSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingResponse response = notificationService.updateSettings(NotificationSettingRequest.builder()
                .vaccineEnabled(false)
                .build());

        assertThat(response.getVaccineEnabled()).isFalse();
        assertThat(response.getVaccineInAppEnabled()).isFalse();
        assertThat(response.getVaccinePushEnabled()).isFalse();
    }

    @Test
    void createNotificationSkipsBothChannelsWhenCategoryIsDisabled() {
        NotificationSetting setting = defaultSetting();
        setting.setVaccineEnabled(false);
        setting.setVaccineInAppEnabled(false);
        setting.setVaccinePushEnabled(false);
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));

        notificationService.createNotification(
                USER_ID,
                Constants.NOTIFICATION_TYPE.VACCINE_REMINDER,
                "Lịch tiêm",
                "Sắp đến lịch tiêm",
                "{}",
                Constants.NOTIFICATION_PRIORITY.NORMAL,
                "VACCINE_RECORD",
                99L,
                true
        );

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(applicationTaskExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void createNotificationUsesBothChannelsWhenCategoryIsEnabled() {
        NotificationSetting setting = defaultSetting();
        when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(100L);
            return notification;
        });

        notificationService.createNotification(
                USER_ID,
                Constants.NOTIFICATION_TYPE.VACCINE_REMINDER,
                "Lịch tiêm",
                "Sắp đến lịch tiêm",
                "{}",
                Constants.NOTIFICATION_PRIORITY.NORMAL,
                "VACCINE_RECORD",
                99L,
                true
        );

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(Constants.TABLE_STATUS.PENDING);
        verify(applicationTaskExecutor).execute(any(Runnable.class));
    }

    private NotificationSetting defaultSetting() {
        return NotificationSetting.builder()
                .user(user)
                .pushEnabled(true)
                .vaccineEnabled(true)
                .appointmentEnabled(true)
                .chatEnabled(true)
                .promotionEnabled(false)
                .systemEnabled(true)
                .vaccineInAppEnabled(true)
                .vaccinePushEnabled(true)
                .appointmentInAppEnabled(true)
                .appointmentPushEnabled(true)
                .chatInAppEnabled(true)
                .chatPushEnabled(true)
                .promotionInAppEnabled(false)
                .promotionPushEnabled(false)
                .systemInAppEnabled(true)
                .systemPushEnabled(true)
                .build();
    }
}
