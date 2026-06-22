package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.DeviceTokenRequest;
import com.example.babyoi_be.domain.dto.request.NotificationSettingRequest;
import com.example.babyoi_be.domain.dto.respone.NotificationResponse;
import com.example.babyoi_be.domain.dto.respone.NotificationSettingResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.entity.DeviceToken;
import com.example.babyoi_be.domain.entity.Notification;
import com.example.babyoi_be.domain.entity.NotificationSetting;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.DeviceTokenRepository;
import com.example.babyoi_be.repository.NotificationRepository;
import com.example.babyoi_be.repository.NotificationSettingRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UsersRepository usersRepository;
    private final WebClient.Builder webClientBuilder;
    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    @Override
    public PageResponse<NotificationResponse> getNotifications(Integer page, Integer size, boolean archived) {
        Long userId = getCurrentUserId();
        int pageIndex = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 20;
        Page<Notification> notifications = archived
                ? notificationRepository.findByUserIdAndArchivedAtIsNotNullAndStatusNotOrderByCreatedAtDesc(
                        userId, Constants.TABLE_STATUS.DELETED, PageRequest.of(pageIndex, pageSize))
                : notificationRepository.findByUserIdAndArchivedAtIsNullAndStatusNotOrderByCreatedAtDesc(
                        userId, Constants.TABLE_STATUS.DELETED, PageRequest.of(pageIndex, pageSize));

        return PageResponse.<NotificationResponse>builder()
                .content(notifications.getContent().stream().map(this::mapToResponse).toList())
                .page(pageIndex)
                .size(pageSize)
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .first(notifications.isFirst())
                .last(notifications.isLast())
                .build();
    }

    @Override
    public long getUnreadCount() {
        return notificationRepository.countByUserIdAndStatus(getCurrentUserId(), Constants.TABLE_STATUS.PENDING);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!Constants.TABLE_STATUS.SUCCESS.equals(notification.getStatus())) {
            notification.setStatus(Constants.TABLE_STATUS.SUCCESS);
            notification.setReadAt(LocalDateTime.now());
        }

        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void archive(Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        LocalDateTime now = LocalDateTime.now();
        notification.setArchivedAt(now);
        if (notification.getReadAt() == null) {
            notification.setReadAt(now);
            notification.setStatus(Constants.TABLE_STATUS.SUCCESS);
        }
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(
                getCurrentUserId(),
                Constants.TABLE_STATUS.PENDING,
                Constants.TABLE_STATUS.SUCCESS,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public void registerDeviceToken(DeviceTokenRequest request) {
        Users user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        DeviceToken token = deviceTokenRepository.findByToken(request.getToken().trim())
                .or(() -> deviceTokenRepository.findByUserIdAndDeviceId(user.getId(), request.getDeviceId().trim()))
                .orElseGet(() -> DeviceToken.builder()
                        .user(user)
                        .createdAt(now)
                        .build());

        token.setUser(user);
        token.setToken(request.getToken().trim());
        token.setPlatform(request.getPlatform().trim().toUpperCase());
        token.setDeviceId(request.getDeviceId().trim());
        token.setAppVersion(request.getAppVersion());
        token.setStatus(Constants.TABLE_STATUS.ACTIVE);
        token.setUpdatedAt(now);
        token.setLastUsedAt(now);
        deviceTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void deactivateDeviceToken(String deviceId) {
        Long userId = getCurrentUserId();
        deviceTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(token -> {
                    token.setStatus(Constants.TABLE_STATUS.INACTIVE);
                    token.setUpdatedAt(LocalDateTime.now());
                    deviceTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public NotificationSettingResponse getSettings() {
        return mapSetting(getOrCreateSetting(getCurrentUser()));
    }

    @Override
    @Transactional
    public NotificationSettingResponse updateSettings(NotificationSettingRequest request) {
        NotificationSetting setting = getOrCreateSetting(getCurrentUser());
        if (request.getPushEnabled() != null) setting.setPushEnabled(request.getPushEnabled());
        if (request.getVaccineEnabled() != null) {
            setting.setVaccineEnabled(request.getVaccineEnabled());
            setting.setVaccineInAppEnabled(request.getVaccineEnabled());
            setting.setVaccinePushEnabled(request.getVaccineEnabled());
        }
        if (request.getAppointmentEnabled() != null) {
            setting.setAppointmentEnabled(request.getAppointmentEnabled());
            setting.setAppointmentInAppEnabled(request.getAppointmentEnabled());
            setting.setAppointmentPushEnabled(request.getAppointmentEnabled());
        }
        if (request.getChatEnabled() != null) {
            setting.setChatEnabled(request.getChatEnabled());
            setting.setChatInAppEnabled(request.getChatEnabled());
            setting.setChatPushEnabled(request.getChatEnabled());
        }
        if (request.getPromotionEnabled() != null) {
            setting.setPromotionEnabled(request.getPromotionEnabled());
            setting.setPromotionInAppEnabled(request.getPromotionEnabled());
            setting.setPromotionPushEnabled(request.getPromotionEnabled());
        }
        if (request.getSystemEnabled() != null) {
            setting.setSystemEnabled(request.getSystemEnabled());
            setting.setSystemInAppEnabled(request.getSystemEnabled());
            setting.setSystemPushEnabled(request.getSystemEnabled());
        }
        if (request.getVaccineInAppEnabled() != null) setting.setVaccineInAppEnabled(request.getVaccineInAppEnabled());
        if (request.getVaccinePushEnabled() != null) setting.setVaccinePushEnabled(request.getVaccinePushEnabled());
        if (request.getAppointmentInAppEnabled() != null) setting.setAppointmentInAppEnabled(request.getAppointmentInAppEnabled());
        if (request.getAppointmentPushEnabled() != null) setting.setAppointmentPushEnabled(request.getAppointmentPushEnabled());
        if (request.getChatInAppEnabled() != null) setting.setChatInAppEnabled(request.getChatInAppEnabled());
        if (request.getChatPushEnabled() != null) setting.setChatPushEnabled(request.getChatPushEnabled());
        if (request.getPromotionInAppEnabled() != null) setting.setPromotionInAppEnabled(request.getPromotionInAppEnabled());
        if (request.getPromotionPushEnabled() != null) setting.setPromotionPushEnabled(request.getPromotionPushEnabled());
        if (request.getSystemInAppEnabled() != null) setting.setSystemInAppEnabled(request.getSystemInAppEnabled());
        if (request.getSystemPushEnabled() != null) setting.setSystemPushEnabled(request.getSystemPushEnabled());
        syncLegacyCategoryFlags(setting);
        setting.setUpdatedAt(LocalDateTime.now());
        return mapSetting(notificationSettingRepository.save(setting));
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String type, String title, String body, String dataJson,
                                   Long priority, String sourceType, Long sourceId, boolean push) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean createInApp = shouldCreateInApp(userId, type);
        boolean sendPush = push && shouldSendPush(userId, type);
        if (!createInApp && !sendPush) {
            return;
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .dataJson(dataJson)
                .priority(priority != null ? priority : Constants.NOTIFICATION_PRIORITY.NORMAL)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .status(createInApp ? Constants.TABLE_STATUS.PENDING : Constants.TABLE_STATUS.DELETED)
                .createdAt(LocalDateTime.now())
                .build());

        if (sendPush) {
            schedulePush(user.getId(), notification.getId());
        }
    }

    @Override
    @Transactional
    public boolean createReminderNotification(Long userId, String type, String title, String body, String dataJson,
                                              Long priority, String sourceType, Long sourceId, String reminderKey) {
        if (!usersRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        boolean createInApp = shouldCreateInApp(userId, type);
        boolean sendPush = shouldSendPush(userId, type);
        if (!createInApp && !sendPush) {
            return false;
        }
        int inserted = notificationRepository.insertReminder(
                userId, type, title, body, dataJson,
                priority != null ? priority : Constants.NOTIFICATION_PRIORITY.NORMAL,
                sourceType, sourceId, reminderKey, createInApp ? Constants.TABLE_STATUS.PENDING : Constants.TABLE_STATUS.DELETED
        );
        if (inserted == 0) {
            return false;
        }
        Notification notification = notificationRepository.findByReminderKey(reminderKey)
                .orElseThrow(() -> new IllegalStateException("Created reminder not found"));
        if (sendPush) {
            schedulePush(userId, notification.getId());
        }
        return true;
    }

    private void schedulePush(Long userId, Long notificationId) {
        Runnable task = () -> sendPush(userId, notificationId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationTaskExecutor.execute(task);
                }
            });
            return;
        }
        applicationTaskExecutor.execute(task);
    }

    private boolean shouldSendPush(Long userId, String type) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId).orElse(null);
        if (setting == null) {
            return true;
        }
        if (!Boolean.TRUE.equals(setting.getPushEnabled())) {
            return false;
        }
        return resolveCategoryPushEnabled(setting, type);
    }

    private boolean shouldCreateInApp(Long userId, String type) {
        return notificationSettingRepository.findByUserId(userId)
                .map(setting -> resolveCategoryInAppEnabled(setting, type))
                .orElse(true);
    }

    private boolean resolveCategoryInAppEnabled(NotificationSetting setting, String type) {
        if (Constants.NOTIFICATION_TYPE.VACCINE_REMINDER.equals(type)) {
            return resolve(setting.getVaccineInAppEnabled(), setting.getVaccineEnabled(), true);
        }
        if (Constants.NOTIFICATION_TYPE.HANDBOOK_COMMENT_REPLY.equals(type)) {
            return resolve(setting.getChatInAppEnabled(), setting.getChatEnabled(), true);
        }
        return resolve(setting.getSystemInAppEnabled(), setting.getSystemEnabled(), true);
    }

    private boolean resolveCategoryPushEnabled(NotificationSetting setting, String type) {
        if (Constants.NOTIFICATION_TYPE.VACCINE_REMINDER.equals(type)) {
            return resolve(setting.getVaccinePushEnabled(), setting.getVaccineEnabled(), true);
        }
        if (Constants.NOTIFICATION_TYPE.HANDBOOK_COMMENT_REPLY.equals(type)) {
            return resolve(setting.getChatPushEnabled(), setting.getChatEnabled(), true);
        }
        return resolve(setting.getSystemPushEnabled(), setting.getSystemEnabled(), true);
    }

    private Boolean resolve(Boolean value, Boolean fallback, boolean defaultValue) {
        if (value != null) {
            return value;
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultValue;
    }

    private void syncLegacyCategoryFlags(NotificationSetting setting) {
        setting.setVaccineEnabled(
                resolve(setting.getVaccineInAppEnabled(), setting.getVaccineEnabled(), true)
                        && resolve(setting.getVaccinePushEnabled(), setting.getVaccineEnabled(), true)
        );
        setting.setAppointmentEnabled(
                resolve(setting.getAppointmentInAppEnabled(), setting.getAppointmentEnabled(), true)
                        && resolve(setting.getAppointmentPushEnabled(), setting.getAppointmentEnabled(), true)
        );
        setting.setChatEnabled(
                resolve(setting.getChatInAppEnabled(), setting.getChatEnabled(), true)
                        && resolve(setting.getChatPushEnabled(), setting.getChatEnabled(), true)
        );
        setting.setPromotionEnabled(
                resolve(setting.getPromotionInAppEnabled(), setting.getPromotionEnabled(), false)
                        && resolve(setting.getPromotionPushEnabled(), setting.getPromotionEnabled(), false)
        );
        setting.setSystemEnabled(
                resolve(setting.getSystemInAppEnabled(), setting.getSystemEnabled(), true)
                        && resolve(setting.getSystemPushEnabled(), setting.getSystemEnabled(), true)
        );
    }

    private void sendPush(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndStatus(userId, Constants.TABLE_STATUS.ACTIVE);
        if (tokens.isEmpty()) {
            return;
        }

        WebClient client = webClientBuilder.baseUrl("https://exp.host").build();
        tokens.forEach(token -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", token.getToken());
            payload.put("title", notification.getTitle());
            payload.put("body", notification.getBody());
            payload.put("sound", "default");
            payload.put("data", Map.of(
                    "notificationId", String.valueOf(notification.getId()),
                    "type", notification.getType(),
                    "dataJson", notification.getDataJson() != null ? notification.getDataJson() : ""
            ));

            try {
                client.post()
                        .uri("/--/api/v2/push/send")
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(5))
                        .block();
                token.setLastUsedAt(LocalDateTime.now());
                deviceTokenRepository.save(token);
            } catch (Exception ignored) {
                token.setStatus(Constants.TABLE_STATUS.FAILED);
                token.setUpdatedAt(LocalDateTime.now());
                deviceTokenRepository.save(token);
            }
        });
    }

    private NotificationSetting getOrCreateSetting(Users user) {
        return notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> notificationSettingRepository.save(NotificationSetting.builder()
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
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    private Users getCurrentUser() {
        return usersRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized"));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized");
        }
        return userDetails.getId();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .dataJson(notification.getDataJson())
                .priority(notification.getPriority())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .status(notification.getStatus())
                .read(Constants.TABLE_STATUS.SUCCESS.equals(notification.getStatus()))
                .readAt(notification.getReadAt())
                .archivedAt(notification.getArchivedAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationSettingResponse mapSetting(NotificationSetting setting) {
        return NotificationSettingResponse.builder()
                .pushEnabled(setting.getPushEnabled())
                .vaccineEnabled(setting.getVaccineEnabled())
                .appointmentEnabled(setting.getAppointmentEnabled())
                .chatEnabled(setting.getChatEnabled())
                .promotionEnabled(setting.getPromotionEnabled())
                .systemEnabled(setting.getSystemEnabled())
                .vaccineInAppEnabled(resolve(setting.getVaccineInAppEnabled(), setting.getVaccineEnabled(), true))
                .vaccinePushEnabled(resolve(setting.getVaccinePushEnabled(), setting.getVaccineEnabled(), true))
                .appointmentInAppEnabled(resolve(setting.getAppointmentInAppEnabled(), setting.getAppointmentEnabled(), true))
                .appointmentPushEnabled(resolve(setting.getAppointmentPushEnabled(), setting.getAppointmentEnabled(), true))
                .chatInAppEnabled(resolve(setting.getChatInAppEnabled(), setting.getChatEnabled(), true))
                .chatPushEnabled(resolve(setting.getChatPushEnabled(), setting.getChatEnabled(), true))
                .promotionInAppEnabled(resolve(setting.getPromotionInAppEnabled(), setting.getPromotionEnabled(), false))
                .promotionPushEnabled(resolve(setting.getPromotionPushEnabled(), setting.getPromotionEnabled(), false))
                .systemInAppEnabled(resolve(setting.getSystemInAppEnabled(), setting.getSystemEnabled(), true))
                .systemPushEnabled(resolve(setting.getSystemPushEnabled(), setting.getSystemEnabled(), true))
                .build();
    }
}
