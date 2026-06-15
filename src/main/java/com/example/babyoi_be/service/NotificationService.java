package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.DeviceTokenRequest;
import com.example.babyoi_be.domain.dto.request.NotificationSettingRequest;
import com.example.babyoi_be.domain.dto.respone.NotificationResponse;
import com.example.babyoi_be.domain.dto.respone.NotificationSettingResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;

public interface NotificationService {
    PageResponse<NotificationResponse> getNotifications(Integer page, Integer size, boolean archived);

    long getUnreadCount();

    NotificationResponse markRead(Long id);

    void archive(Long id);

    void markAllRead();

    void registerDeviceToken(DeviceTokenRequest request);

    void deactivateDeviceToken(String deviceId);

    NotificationSettingResponse getSettings();

    NotificationSettingResponse updateSettings(NotificationSettingRequest request);

    void createNotification(Long userId, String type, String title, String body, String dataJson,
                            Long priority, String sourceType, Long sourceId, boolean push);

    boolean createReminderNotification(Long userId, String type, String title, String body, String dataJson,
                                       Long priority, String sourceType, Long sourceId, String reminderKey);
}
